package io.github.gabrielivo.oficina.billing.domain.cobranca;

import java.time.Instant;

public record ResultadoPreferenciaGateway(
        String idExterno, String linkPagamento, Instant expiraEm, ResultadoPagamentoGateway pagamentoInicial) {
    public ResultadoPreferenciaGateway(String idExterno, String linkPagamento, Instant expiraEm) {
        this(idExterno, linkPagamento, expiraEm, null);
    }
}
