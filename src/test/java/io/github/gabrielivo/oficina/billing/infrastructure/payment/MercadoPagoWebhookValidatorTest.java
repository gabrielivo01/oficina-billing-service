package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.application.webhook.AssinaturaWebhookInvalidaException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookValidatorTest {

    @Test
    void aceitaAssinaturaHmacValidaERejeitaHashAlterado() throws Exception {
        String secret = "webhook-secret-test";
        String dataId = "123456789";
        String requestId = "request-123";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        String hash = hmacSha256(secret, manifest);
        var validator = new MercadoPagoWebhookValidator(new BillingProperties(
                null,
                new BillingProperties.Pagamento(
                        "mercadopago",
                        null,
                        new BillingProperties.MercadoPago(
                                "token", secret, "https://example.test/hook", "https://example.test")),
                null));

        assertDoesNotThrow(() -> validator.validar("ts=" + timestamp + ",v1=" + hash, requestId, dataId));
        assertThrows(
                AssinaturaWebhookInvalidaException.class,
                () -> validator.validar("ts=" + timestamp + ",v1=invalid", requestId, dataId));
    }

    private String hmacSha256(String secret, String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        var hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append("%02x".formatted(value & 0xff));
        }
        return hex.toString();
    }
}
