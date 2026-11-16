package io.github.gabrielivo.oficina.billing.application.orcamento;

import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.math.BigDecimal;
import java.util.List;

public record GerarOrcamentoCommand(
        String osId,
        String sagaId,
        ClienteSnapshot cliente,
        VeiculoSnapshot veiculo,
        List<ItemOrcamento> itens,
        BigDecimal valorTotal) {}
