package io.github.gabrielivo.oficina.billing.infrastructure.messaging;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutboxRepository;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicadorOutbox {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorOutbox.class);
    private final MensagemOutboxRepository repository;
    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final BillingProperties properties;

    public PublicadorOutbox(
            MensagemOutboxRepository repository,
            OutboxService outboxService,
            RabbitTemplate rabbitTemplate,
            BillingProperties properties) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.mensageria.outbox.intervalo:1s}")
    public void publicarPendentes() {
        if (!properties.mensageria().outbox().publicadorHabilitado()) {
            return;
        }
        var mensagens = repository.findBySentAtIsNullOrderByCreatedAtAsc(
                PageRequest.of(0, properties.mensageria().outbox().lote()));
        for (MensagemOutbox mensagem : mensagens) {
            publicar(mensagem);
        }
    }

    private void publicar(MensagemOutbox mensagem) {
        CorrelationData correlation = new CorrelationData(mensagem.getId());
        try {
            rabbitTemplate.convertAndSend(
                    mensagem.getExchange(),
                    mensagem.getRoutingKey(),
                    mensagem.getPayload(),
                    brokerMessage -> {
                        if (mensagem.getTraceParent() != null) {
                            brokerMessage.getMessageProperties().setHeader("traceparent", mensagem.getTraceParent());
                        }
                        return brokerMessage;
                    },
                    correlation);
            var confirm = correlation
                    .getFuture()
                    .get(properties.mensageria().outbox().timeoutConfirmacao().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejeitou a mensagem: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("Mensagem do outbox não foi roteada para nenhuma fila.");
            }
            outboxService.marcarEnviada(mensagem.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            outboxService.registrarFalha(mensagem.getId());
            LOGGER.error("Publicação interrompida para mensagem outbox {}", mensagem.getId(), e);
        } catch (AmqpException | ExecutionException | TimeoutException | IllegalStateException e) {
            outboxService.registrarFalha(mensagem.getId());
            LOGGER.error("Falha ao publicar mensagem outbox {}; será tentada novamente", mensagem.getId(), e);
        }
    }
}
