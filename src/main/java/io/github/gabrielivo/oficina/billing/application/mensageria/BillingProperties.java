package io.github.gabrielivo.oficina.billing.application.mensageria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record BillingProperties(Mensageria mensageria, Pagamento pagamento, Notificacao notificacao) {

    public record Mensageria(String exchangeComandos, String exchangeEventos, String filaComandos, Outbox outbox) {}

    public record Outbox(boolean publicadorHabilitado, int lote, Duration timeoutConfirmacao) {}

    public record Pagamento(String gateway, Fake fake, MercadoPago mercadopago) {}

    public record Fake(String statusInicial) {}

    public record MercadoPago(String accessToken, String webhookSecret, String notificationUrl, String backUrl) {}

    public record Notificacao(String remetente) {}
}
