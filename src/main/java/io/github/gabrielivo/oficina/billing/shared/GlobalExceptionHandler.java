package io.github.gabrielivo.oficina.billing.shared;

import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoNaoEncontradoException;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaNaoEncontradaException;
import io.github.gabrielivo.oficina.billing.application.webhook.AssinaturaWebhookInvalidaException;
import io.github.gabrielivo.oficina.billing.application.webhook.WebhookIndisponivelException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({OrcamentoNaoEncontradoException.class, CobrancaNaoEncontradaException.class})
    ResponseEntity<Map<String, String>> naoEncontrado(RuntimeException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, String>> naoAutorizado(SecurityException exception) {
        return resposta(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(AssinaturaWebhookInvalidaException.class)
    ResponseEntity<Map<String, String>> assinaturaInvalida(AssinaturaWebhookInvalidaException exception) {
        return resposta(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(WebhookIndisponivelException.class)
    ResponseEntity<Map<String, String>> webhookIndisponivel(WebhookIndisponivelException exception) {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> requisicaoInvalida(IllegalArgumentException exception) {
        return resposta(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflito(IllegalStateException exception) {
        return resposta(HttpStatus.CONFLICT, exception);
    }

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(Map.of("erro", exception.getMessage()));
    }
}
