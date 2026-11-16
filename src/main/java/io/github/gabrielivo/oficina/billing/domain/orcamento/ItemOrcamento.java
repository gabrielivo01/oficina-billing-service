package io.github.gabrielivo.oficina.billing.domain.orcamento;

import java.math.BigDecimal;

public record ItemOrcamento(
        String tipo, String descricao, String pecaId, int quantidade, BigDecimal valorUnitario, BigDecimal valorTotal) {

    public ItemOrcamento {
        if (tipo == null || descricao == null || descricao.isBlank() || quantidade <= 0) {
            throw new IllegalArgumentException("Tipo, descrição e quantidade válida são obrigatórios.");
        }
        if (valorUnitario == null || valorUnitario.signum() < 0 || valorTotal == null || valorTotal.signum() < 0) {
            throw new IllegalArgumentException("Valores do item devem ser não negativos.");
        }
        if (valorUnitario.multiply(BigDecimal.valueOf(quantidade)).compareTo(valorTotal) != 0) {
            throw new IllegalArgumentException(
                    "O total do item deve corresponder ao preço unitário multiplicado pela quantidade.");
        }
        if ("PECA".equals(tipo) && (pecaId == null || pecaId.isBlank())) {
            throw new IllegalArgumentException("pecaId é obrigatório para item PECA.");
        }
    }
}
