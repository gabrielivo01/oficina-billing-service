package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import io.github.gabrielivo.oficina.billing.application.webhook.ValidadorWebhookPagamento;
import io.github.gabrielivo.oficina.billing.application.webhook.WebhookIndisponivelException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "fake", matchIfMissing = true)
public class WebhookIndisponivelValidator implements ValidadorWebhookPagamento {

    @Override
    public void validar(String assinatura, String requestId, String dataId) {
        throw new WebhookIndisponivelException();
    }
}
