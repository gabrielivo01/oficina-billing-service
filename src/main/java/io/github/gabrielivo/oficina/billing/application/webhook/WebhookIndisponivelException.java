package io.github.gabrielivo.oficina.billing.application.webhook;

public class WebhookIndisponivelException extends RuntimeException {

    public WebhookIndisponivelException() {
        super("Webhook Mercado Pago indisponível para o gateway configurado.");
    }
}
