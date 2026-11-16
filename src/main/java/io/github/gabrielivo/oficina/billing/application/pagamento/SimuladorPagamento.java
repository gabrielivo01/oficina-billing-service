package io.github.gabrielivo.oficina.billing.application.pagamento;

import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;

public interface SimuladorPagamento {

    void definirStatus(String osId, StatusPagamentoGateway status);
}
