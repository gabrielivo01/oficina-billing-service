package io.github.gabrielivo.oficina.billing.application.orcamento;

import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;

public interface NotificadorOrcamento {

    void notificar(Orcamento orcamento);
}
