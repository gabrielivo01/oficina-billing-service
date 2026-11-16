package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificadorCobrancasPendentesTest {

    private final CobrancaService service = mock(CobrancaService.class);
    private final VerificadorCobrancasPendentes verificador = new VerificadorCobrancasPendentes(service);

    @Test
    void deveConsultarNoGatewayCadaCobrancaPendente() {
        when(service.listarIdsPendentes()).thenReturn(List.of("os-1", "os-2"));

        verificador.consultar();

        var ordem = inOrder(service);
        ordem.verify(service).verificarPagamentoPendente("os-1");
        ordem.verify(service).verificarPagamentoPendente("os-2");
    }

    @Test
    void semCobrancasPendentesNaoDeveConsultarOGateway() {
        when(service.listarIdsPendentes()).thenReturn(List.of());

        verificador.consultar();

        verify(service, never()).verificarPagamentoPendente(any());
    }
}
