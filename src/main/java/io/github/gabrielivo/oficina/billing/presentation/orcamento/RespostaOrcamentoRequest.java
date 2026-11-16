package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespostaOrcamentoRequest(
        @NotNull Boolean aprovado, @Size(max = 1000) String observacao) {}
