package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import io.github.gabrielivo.oficina.billing.domain.orcamento.StatusOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrcamentoResponse(
        String id,
        String osId,
        Cliente cliente,
        Veiculo veiculo,
        List<Item> itens,
        BigDecimal valorTotal,
        StatusOrcamento status,
        Instant criadoEm,
        Instant respondidoEm,
        String respondidoPorTipo,
        String observacao) {

    public record Cliente(String id, String nome, String cpf, String telefone, String email) {}

    public record Veiculo(String placa, String marca, String modelo, Integer ano) {}

    public record Item(
            String tipo,
            String descricao,
            String pecaId,
            int quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal) {}
}
