package io.github.gabrielivo.oficina.billing.domain.cobranca;

import java.math.BigDecimal;

public record ResultadoPagamentoGateway(
        String osId,
        String pagamentoId,
        StatusPagamentoGateway status,
        BigDecimal valor,
        String formaPagamento,
        String motivo) {}
