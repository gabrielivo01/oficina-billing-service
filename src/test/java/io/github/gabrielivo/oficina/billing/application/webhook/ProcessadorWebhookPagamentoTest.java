package io.github.gabrielivo.oficina.billing.application.webhook;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessadorWebhookPagamentoTest {

    @Mock
    private ValidadorWebhookPagamento validador;

    @Mock
    private GatewayPagamento gateway;

    @Mock
    private CobrancaService cobrancaService;

    @Test
    void validaAssinaturaAntesDeConsultarEAtualizarEstado() {
        var pagamento = new ResultadoPagamentoGateway(
                "os-1", "123", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null);
        when(gateway.consultarPagamentoPorId("123")).thenReturn(pagamento);
        var processor = new ProcessadorWebhookPagamento(validador, gateway, cobrancaService);

        processor.processar("signature", "request", "123");

        InOrder ordem = inOrder(validador, gateway, cobrancaService);
        ordem.verify(validador).validar("signature", "request", "123");
        ordem.verify(gateway).consultarPagamentoPorId("123");
        ordem.verify(cobrancaService).atualizarPagamento(pagamento);
    }

    @Test
    void rejeitaPagamentoConsultadoComIdDiferenteDoWebhook() {
        when(gateway.consultarPagamentoPorId("123"))
                .thenReturn(new ResultadoPagamentoGateway(
                        "os-1", "456", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null));
        var processor = new ProcessadorWebhookPagamento(validador, gateway, cobrancaService);

        assertThrows(SecurityException.class, () -> processor.processar("signature", "request", "123"));
        verify(cobrancaService, never()).atualizarPagamento(org.mockito.ArgumentMatchers.any());
    }
}
