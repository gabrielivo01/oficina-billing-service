package io.github.gabrielivo.oficina.billing.presentation.pagamento;

import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.SimuladorPagamento;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cobrancas")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "fake", matchIfMissing = true)
public class PagamentoFakeController {

    private final SimuladorPagamento simulador;
    private final CobrancaService cobrancaService;

    public PagamentoFakeController(SimuladorPagamento simulador, CobrancaService cobrancaService) {
        this.simulador = simulador;
        this.cobrancaService = cobrancaService;
    }

    @PostMapping("/{osId}/simular-pagamento")
    public void simular(@PathVariable String osId, @Valid @RequestBody PagamentoFakeRequest request) {
        simulador.definirStatus(osId, request.status());
        cobrancaService.verificarPagamentoPendente(osId);
    }
}
