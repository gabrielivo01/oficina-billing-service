package io.github.gabrielivo.oficina.billing.application.webhook;

public interface ValidadorWebhookPagamento {

    void validar(String assinatura, String requestId, String dataId);
}
