package io.github.gabrielivo.oficina.billing.presentation.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.gabrielivo.oficina.billing.application.webhook.ProcessadorWebhookPagamento;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class WebhookMercadoPagoControllerTest {

    @Test
    void ignoraEventosNaoPagamentoESolicitacoesSemId() {
        ProcessadorWebhookPagamento processador = Mockito.mock(ProcessadorWebhookPagamento.class);
        WebhookMercadoPagoController controller = new WebhookMercadoPagoController(processador);

        assertEquals(
                HttpStatus.NO_CONTENT,
                controller.receber(null, null, "1", "merchant_order").getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                controller.receber(null, null, " ", "payment").getStatusCode());
        verifyNoInteractions(processador);
    }

    @Test
    void delegaPagamentoAssinadoERetornaAccepted() {
        ProcessadorWebhookPagamento processador = Mockito.mock(ProcessadorWebhookPagamento.class);
        WebhookMercadoPagoController controller = new WebhookMercadoPagoController(processador);

        assertEquals(
                HttpStatus.ACCEPTED,
                controller.receber("signature", "request", "123", "payment").getStatusCode());
        verify(processador).processar("signature", "request", "123");
    }
}
