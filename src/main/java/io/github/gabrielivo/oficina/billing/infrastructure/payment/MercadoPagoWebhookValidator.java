package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.application.webhook.AssinaturaWebhookInvalidaException;
import io.github.gabrielivo.oficina.billing.application.webhook.ValidadorWebhookPagamento;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "mercadopago")
public class MercadoPagoWebhookValidator implements ValidadorWebhookPagamento {

    private final BillingProperties properties;

    public MercadoPagoWebhookValidator(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public void validar(String assinatura, String requestId, String dataId) {
        try {
            WebhookSignatureValidator.validate(
                    assinatura,
                    requestId,
                    dataId,
                    properties.pagamento().mercadopago().webhookSecret(),
                    Duration.ofMinutes(5));
        } catch (MPInvalidWebhookSignatureException e) {
            throw new AssinaturaWebhookInvalidaException(e);
        }
    }
}
