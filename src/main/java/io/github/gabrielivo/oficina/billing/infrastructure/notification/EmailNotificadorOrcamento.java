package io.github.gabrielivo.oficina.billing.infrastructure.notification;

import io.github.gabrielivo.oficina.billing.application.orcamento.NotificadorOrcamento;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmail;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmailRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificadorOrcamento implements NotificadorOrcamento {

    private final NotificacaoEmailRepository repository;

    public EmailNotificadorOrcamento(NotificacaoEmailRepository repository) {
        this.repository = repository;
    }

    @Override
    public void notificar(Orcamento orcamento) {
        repository.save(new NotificacaoEmail(
                orcamento.getCliente().email(),
                "Orçamento da ordem de serviço " + orcamento.getOsId(),
                "Olá, "
                        + orcamento.getCliente().nome()
                        + ". O orçamento da sua ordem de serviço é de R$ "
                        + orcamento.getValorTotal()
                        + ". Acesse a oficina para aprovar ou recusar."));
    }
}
