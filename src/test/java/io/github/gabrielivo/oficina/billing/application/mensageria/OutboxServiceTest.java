package io.github.gabrielivo.oficina.billing.application.mensageria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutbox;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemOutboxRepository;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private MensagemOutboxRepository repository;

    @Mock
    private ContextoRastreamento contextoRastreamento;

    private BillingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BillingProperties(
                new BillingProperties.Mensageria(
                        "oficina.comandos",
                        "oficina.eventos",
                        "billing.comandos",
                        new BillingProperties.Outbox(true, 10, Duration.ofSeconds(1))),
                null,
                null);
    }

    @Test
    void persisteMarcacaoDeEnvioEFalha() {
        var mensagem = new MensagemOutbox("PagamentoConfirmado", "eventos", "billing.pagamento-confirmado", "{}", null);
        when(repository.findById(mensagem.getId())).thenReturn(Optional.of(mensagem));
        var service =
                new OutboxService(repository, properties, JsonMapper.builder().build(), contextoRastreamento);

        service.marcarEnviada(mensagem.getId());
        assertNotNull(mensagem.getSentAt());
        verify(repository).save(mensagem);

        mensagem = new MensagemOutbox("PagamentoRejeitado", "eventos", "billing.pagamento-rejeitado", "{}", null);
        when(repository.findById(mensagem.getId())).thenReturn(Optional.of(mensagem));
        service.registrarFalha(mensagem.getId());
        assertEquals(1, mensagem.getAttempts());
        verify(repository).save(mensagem);
    }

    @Test
    void registraEventoComEnvelopeRoteamentoTraceEPayload() throws Exception {
        when(contextoRastreamento.traceParentAtual()).thenReturn("00-trace-span-01");
        var service =
                new OutboxService(repository, properties, JsonMapper.builder().build(), contextoRastreamento);

        service.registrar("PagamentoEstornado", "saga-1", "os-1", Map.of("refundId", "refund-1"));

        ArgumentCaptor<MensagemOutbox> captor = ArgumentCaptor.forClass(MensagemOutbox.class);
        verify(repository).save(captor.capture());
        MensagemOutbox mensagem = captor.getValue();
        assertEquals("oficina.eventos", mensagem.getExchange());
        assertEquals("billing.pagamento-estornado", mensagem.getRoutingKey());
        assertEquals("00-trace-span-01", mensagem.getTraceParent());
        EnvelopeMensagem envelope =
                JsonMapper.builder().build().readValue(mensagem.getPayload(), EnvelopeMensagem.class);
        assertEquals("PagamentoEstornado", envelope.type());
        assertEquals("saga-1", envelope.sagaId());
        assertEquals("os-1", envelope.osId());
        assertEquals("refund-1", envelope.payload().get("refundId"));
    }

    @Test
    void falhaExplicitamenteQuandoMensagemOutboxNaoExiste() {
        var service =
                new OutboxService(repository, properties, JsonMapper.builder().build(), contextoRastreamento);

        assertThrows(IllegalStateException.class, () -> service.marcarEnviada("missing"));
        assertThrows(IllegalStateException.class, () -> service.registrarFalha("missing"));
        verify(repository, never()).save(any(MensagemOutbox.class));
    }
}
