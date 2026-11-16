package io.github.gabrielivo.oficina.billing.presentation.cobranca;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.IdentidadeAtual;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CobrancaControllerTest {

    @Mock
    private CobrancaService service;

    @Mock
    private CobrancaMapper mapper;

    @Mock
    private IdentidadeAtual identidadeAtual;

    @Test
    void buscaCobrancaComIdentidadeAtual() {
        var identidade = new IdentidadeAutenticada("cliente-1", IdentidadeAutenticada.TipoIdentidade.CLIENTE);
        Cobranca cobranca = org.mockito.Mockito.mock(Cobranca.class);
        CobrancaResponse response = new CobrancaResponse(null, null, null, null, null, null, null, null, null);
        when(identidadeAtual.obter()).thenReturn(identidade);
        when(service.buscarPorOs("os-1", identidade)).thenReturn(cobranca);
        when(mapper.paraResponse(cobranca)).thenReturn(response);
        CobrancaController controller = new CobrancaController(service, mapper, identidadeAtual);

        assertSame(response, controller.buscar("os-1"));
    }
}
