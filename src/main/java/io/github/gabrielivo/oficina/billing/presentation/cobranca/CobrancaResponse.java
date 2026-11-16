package io.github.gabrielivo.oficina.billing.presentation.cobranca;

import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusCobranca;
import java.math.BigDecimal;
import java.time.Instant;

public record CobrancaResponse(
        String id,
        String osId,
        String orcamentoId,
        BigDecimal valor,
        StatusCobranca status,
        String linkPagamento,
        Instant expiraEm,
        String pagamentoIdExterno,
        String formaPagamento) {}
