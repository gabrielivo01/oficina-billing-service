package io.github.gabrielivo.oficina.billing.presentation.webhook;

import io.github.gabrielivo.oficina.billing.application.webhook.ProcessadorWebhookPagamento;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/mercadopago")
public class WebhookMercadoPagoController {

    private final ProcessadorWebhookPagamento processador;

    public WebhookMercadoPagoController(ProcessadorWebhookPagamento processador) {
        this.processador = processador;
    }

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(name = "x-signature", required = false) String assinatura,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "type", required = false) String type) {
        if (!"payment".equals(type)) {
            return ResponseEntity.noContent().build();
        }
        if (dataId == null || dataId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        processador.processar(assinatura, requestId, dataId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
