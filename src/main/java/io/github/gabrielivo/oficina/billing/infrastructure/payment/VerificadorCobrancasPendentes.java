package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "mercadopago")
public class VerificadorCobrancasPendentes {

    private final CobrancaService service;

    public VerificadorCobrancasPendentes(CobrancaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.pagamento.intervalo-verificacao:30s}")
    public void consultar() {
        for (String osId : service.listarIdsPendentes()) {
            service.verificarPagamentoPendente(osId);
        }
    }
}
