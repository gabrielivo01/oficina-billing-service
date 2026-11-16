package io.github.gabrielivo.oficina.billing.application.webhook;

import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import org.springframework.stereotype.Service;

@Service
public class ProcessadorWebhookPagamento {

    private final ValidadorWebhookPagamento validador;
    private final GatewayPagamento gateway;
    private final CobrancaService cobrancaService;

    public ProcessadorWebhookPagamento(
            ValidadorWebhookPagamento validador, GatewayPagamento gateway, CobrancaService cobrancaService) {
        this.validador = validador;
        this.gateway = gateway;
        this.cobrancaService = cobrancaService;
    }

    public void processar(String assinatura, String requestId, String dataId) {
        validador.validar(assinatura, requestId, dataId);
        var pagamento = gateway.consultarPagamentoPorId(dataId);
        if (pagamento.pagamentoId() == null || !pagamento.pagamentoId().equals(dataId)) {
            throw new SecurityException("O pagamento consultado não corresponde ao recurso notificado.");
        }
        cobrancaService.atualizarPagamento(pagamento);
    }
}
