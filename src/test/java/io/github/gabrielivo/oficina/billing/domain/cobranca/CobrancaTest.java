package io.github.gabrielivo.oficina.billing.domain.cobranca;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CobrancaTest {

    @Test
    void aprovaUmaVezERejeitaValorDivergente() {
        var cobranca = novaCobranca();
        var aprovada = new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "credit_card", null);

        assertTrue(cobranca.atualizarPagamento(aprovada));
        assertFalse(cobranca.atualizarPagamento(aprovada));
        assertThrows(
                IllegalStateException.class,
                () -> cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                        "os-1",
                        "pag-1",
                        StatusPagamentoGateway.APROVADO,
                        new BigDecimal("19.00"),
                        "credit_card",
                        null)));
    }

    @Test
    void cancelamentoIgnoraAtualizacaoTardiaEEstornoExigeAprovacao() {
        var cobranca = novaCobranca();
        cobranca.cancelar("prazo expirado");

        assertFalse(cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null)));
        assertThrows(IllegalStateException.class, () -> cobranca.estornar("refund-1"));

        var aprovada = novaCobranca();
        aprovada.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null));
        assertThrows(IllegalArgumentException.class, () -> aprovada.estornar(" "));
        aprovada.estornar("refund-1");
        assertFalse(aprovada.atualizarPagamento(
                new ResultadoPagamentoGateway("os-1", "pag-1", StatusPagamentoGateway.REJEITADO, null, "pix", null)));
    }

    @Test
    void validaDadosDaCobrancaETransicoesDePagamentoPendentesOuTerminais() {
        Instant expiry = Instant.now().plusSeconds(600);
        var preferencia = new ResultadoPreferenciaGateway("pref-1", "https://example.test/pay", expiry);
        assertThrows(
                IllegalArgumentException.class,
                () -> new Cobranca(" ", "orc-1", new BigDecimal("20.00"), preferencia, expiry));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Cobranca("os-1", "orc-1", BigDecimal.ZERO, preferencia, expiry));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Cobranca("os-1", "orc-1", new BigDecimal("20.00"), null, expiry));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Cobranca(
                        "os-1",
                        "orc-1",
                        new BigDecimal("20.00"),
                        new ResultadoPreferenciaGateway(" ", "https://example.test/pay", expiry),
                        expiry));

        var cobranca = novaCobranca();
        assertThrows(IllegalArgumentException.class, () -> cobranca.atualizarPagamento(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> cobranca.atualizarPagamento(
                        new ResultadoPagamentoGateway("os-1", "pag-1", null, new BigDecimal("20.00"), "pix", null)));
        assertFalse(cobranca.atualizarPagamento(
                new ResultadoPagamentoGateway("os-1", "pag-1", StatusPagamentoGateway.PENDENTE, null, "pix", null)));
        assertThrows(
                IllegalStateException.class,
                () -> cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                        "os-1", "pag-1", StatusPagamentoGateway.APROVADO, null, "pix", null)));
        assertThrows(
                IllegalStateException.class,
                () -> cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                        "os-1", " ", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null)));

        assertTrue(cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.REJEITADO, null, "pix", "Recusado")));
        assertThrows(
                IllegalStateException.class,
                () -> cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                        "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null)));
        cobranca.cancelar("irrelevante");
    }

    private Cobranca novaCobranca() {
        return new Cobranca(
                "os-1",
                "orc-1",
                new BigDecimal("20.00"),
                new ResultadoPreferenciaGateway(
                        "pref-1", "https://example.test/pay", Instant.now().plusSeconds(600)),
                Instant.now().plusSeconds(600));
    }
}
