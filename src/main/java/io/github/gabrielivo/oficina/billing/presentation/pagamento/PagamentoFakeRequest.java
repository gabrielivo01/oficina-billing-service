package io.github.gabrielivo.oficina.billing.presentation.pagamento;

import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import jakarta.validation.constraints.NotNull;

public record PagamentoFakeRequest(@NotNull StatusPagamentoGateway status) {}
