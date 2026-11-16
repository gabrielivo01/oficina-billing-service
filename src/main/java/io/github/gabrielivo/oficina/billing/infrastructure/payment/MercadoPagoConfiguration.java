package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "mercadopago")
public class MercadoPagoConfiguration {

    public MercadoPagoConfiguration(BillingProperties properties) {
        var mercadopago = properties.pagamento().mercadopago();
        if (isBlank(mercadopago.accessToken())
                || isBlank(mercadopago.webhookSecret())
                || isBlank(mercadopago.notificationUrl())
                || isBlank(mercadopago.backUrl())) {
            throw new IllegalStateException(
                    "MP_ACCESS_TOKEN, MP_WEBHOOK_SECRET, MP_NOTIFICATION_URL e MP_BACK_URL são obrigatórios no perfil Mercado Pago.");
        }
        MercadoPagoConfig.setAccessToken(mercadopago.accessToken());
    }

    @Bean
    PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    @Bean
    PaymentClient paymentClient() {
        return new PaymentClient();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
