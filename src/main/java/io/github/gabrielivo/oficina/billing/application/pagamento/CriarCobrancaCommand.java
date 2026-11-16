package io.github.gabrielivo.oficina.billing.application.pagamento;

import java.time.Instant;

public record CriarCobrancaCommand(
        String osId, String sagaId, String orcamentoId, String descricao, Instant prazoPagamento) {}
