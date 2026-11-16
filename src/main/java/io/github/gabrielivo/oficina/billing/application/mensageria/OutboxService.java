package io.github.gabrielivo.oficina.billing.application.mensageria;

import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutboxRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxService {

    private final MensagemOutboxRepository repository;
    private final BillingProperties properties;
    private final JsonMapper jsonMapper;
    private final ContextoRastreamento contextoRastreamento;

    public OutboxService(
            MensagemOutboxRepository repository,
            BillingProperties properties,
            JsonMapper jsonMapper,
            ContextoRastreamento contextoRastreamento) {
        this.repository = repository;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.contextoRastreamento = contextoRastreamento;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String type, String sagaId, String osId, Map<String, Object> payload) {
        EnvelopeMensagem envelope =
                new EnvelopeMensagem(UUID.randomUUID().toString(), type, sagaId, osId, Instant.now(), payload);
        try {
            repository.save(new MensagemOutbox(
                    type,
                    properties.mensageria().exchangeEventos(),
                    routingKey(type),
                    jsonMapper.writeValueAsString(envelope),
                    contextoRastreamento.traceParentAtual()));
        } catch (JacksonException e) {
            throw new IllegalStateException("Não foi possível serializar o evento " + type + ".", e);
        }
    }

    @Transactional
    public void marcarEnviada(String id) {
        MensagemOutbox mensagem = buscar(id);
        mensagem.markSent();
        repository.save(mensagem);
    }

    @Transactional
    public void registrarFalha(String id) {
        MensagemOutbox mensagem = buscar(id);
        mensagem.registerFailure();
        repository.save(mensagem);
    }

    private MensagemOutbox buscar(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("Mensagem outbox não encontrada: " + id));
    }

    private String routingKey(String type) {
        StringBuilder key = new StringBuilder("billing.");
        for (char character : type.toCharArray()) {
            if (Character.isUpperCase(character) && key.length() > "billing.".length()) {
                key.append('-');
            }
            key.append(Character.toLowerCase(character));
        }
        return key.toString();
    }
}
