package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayPagamentoFakeTest {

    @Test
    void criaPreferenciaDeterministicaESimulaAprovacaoEEstorno() {
        var fake = new GatewayPagamentoFake(new BillingProperties(
                null, new BillingProperties.Pagamento("fake", new BillingProperties.Fake("PENDENTE"), null), null));
        var itens = List.of(
                new ItemOrcamento("SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00")));
        Instant deadline = Instant.now().plusSeconds(600);
        var primeira =
                fake.criarCobranca("os-1", "orc-1", "Revisão", new BigDecimal("20.00"), deadline, itens, cliente());
        var repetida =
                fake.criarCobranca("os-1", "orc-1", "Revisão", new BigDecimal("20.00"), deadline, itens, cliente());

        assertEquals(primeira.idExterno(), repetida.idExterno());
        fake.definirStatus("os-1", StatusPagamentoGateway.APROVADO);
        var pagamento = fake.consultarPagamento("os-1", primeira.idExterno());
        assertEquals(StatusPagamentoGateway.APROVADO, pagamento.status());
        assertEquals(
                "fake-refund-" + pagamento.pagamentoId(),
                fake.estornar("os-1", primeira.idExterno(), pagamento.pagamentoId()));
        assertThrows(
                IllegalArgumentException.class,
                () -> fake.estornar("os-1", primeira.idExterno(), "pagamento-estranho"));
    }

    @Test
    void trataPagamentoInicialAprovadoCancelamentoEOperacoesDeWebhookIndisponiveis() {
        var fake = new GatewayPagamentoFake(new BillingProperties(
                null, new BillingProperties.Pagamento("fake", new BillingProperties.Fake("APROVADO"), null), null));
        Instant deadline = Instant.now().plusSeconds(600);
        var preference =
                fake.criarCobranca("os-2", "orc-2", "Revisão", new BigDecimal("20.00"), deadline, List.of(), cliente());

        assertEquals(
                StatusPagamentoGateway.APROVADO, preference.pagamentoInicial().status());
        assertEquals(
                preference.pagamentoInicial().pagamentoId(),
                fake.consultarPagamento("os-2", preference.idExterno()).pagamentoId());
        assertThrows(IllegalStateException.class, () -> fake.consultarPagamentoPorId("pagamento-1"));

        fake.cancelar("os-2", preference.idExterno());
        assertEquals(
                StatusPagamentoGateway.REJEITADO,
                fake.consultarPagamento("os-2", preference.idExterno()).status());
        assertThrows(
                IllegalStateException.class,
                () -> fake.estornar(
                        "os-2",
                        preference.idExterno(),
                        preference.pagamentoInicial().pagamentoId()));
        assertThrows(
                IllegalArgumentException.class,
                () -> fake.definirStatus("os-inexistente", StatusPagamentoGateway.APROVADO));
        assertThrows(IllegalArgumentException.class, () -> fake.definirStatus("os-2", null));
    }

    private ClienteSnapshot cliente() {
        return new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com");
    }
}
