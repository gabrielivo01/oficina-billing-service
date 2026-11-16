package io.github.gabrielivo.oficina.billing.application.webhook;

public class AssinaturaWebhookInvalidaException extends RuntimeException {

    public AssinaturaWebhookInvalidaException(Throwable cause) {
        super("Assinatura do webhook inválida.", cause);
    }
}
