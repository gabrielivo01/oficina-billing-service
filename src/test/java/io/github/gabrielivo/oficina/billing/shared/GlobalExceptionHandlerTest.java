package io.github.gabrielivo.oficina.billing.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoNaoEncontradoException;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaNaoEncontradaException;
import io.github.gabrielivo.oficina.billing.application.webhook.AssinaturaWebhookInvalidaException;
import io.github.gabrielivo.oficina.billing.application.webhook.WebhookIndisponivelException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void orcamentoOuCobrancaInexistenteDevemVirar404() {
        assertResposta(handler.naoEncontrado(new OrcamentoNaoEncontradoException("orc-1")), HttpStatus.NOT_FOUND);
        assertResposta(handler.naoEncontrado(new CobrancaNaoEncontradaException("os-1")), HttpStatus.NOT_FOUND);
    }

    @Test
    void acessoNegadoDeveVirar403() {
        assertResposta(handler.naoAutorizado(new SecurityException("sem acesso")), HttpStatus.FORBIDDEN);
    }

    @Test
    void assinaturaDeWebhookInvalidaDeveVirar401() {
        assertResposta(
                handler.assinaturaInvalida(new AssinaturaWebhookInvalidaException(new IllegalStateException("x"))),
                HttpStatus.UNAUTHORIZED);
    }

    @Test
    void webhookSemValidadorConfiguradoDeveVirar503() {
        assertResposta(handler.webhookIndisponivel(new WebhookIndisponivelException()), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void entradaInvalidaDeveVirar400EEstadoInvalido409() {
        assertResposta(handler.requisicaoInvalida(new IllegalArgumentException("campo")), HttpStatus.BAD_REQUEST);
        assertResposta(handler.conflito(new IllegalStateException("estado")), HttpStatus.CONFLICT);
    }

    private static void assertResposta(ResponseEntity<Map<String, String>> resposta, HttpStatus esperado) {
        assertThat(resposta.getStatusCode()).isEqualTo(esperado);
        assertThat(resposta.getBody()).containsKey("erro");
    }
}
