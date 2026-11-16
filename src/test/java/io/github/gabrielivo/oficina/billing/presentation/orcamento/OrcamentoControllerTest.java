package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.IdentidadeAtual;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoService;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    @Mock
    private OrcamentoService service;

    @Mock
    private OrcamentoMapper mapper;

    @Mock
    private IdentidadeAtual identidadeAtual;

    private OrcamentoController controller;
    private IdentidadeAutenticada identidade;
    private Orcamento orcamento;
    private OrcamentoResponse response;

    @BeforeEach
    void setUp() {
        controller = new OrcamentoController(service, mapper, identidadeAtual);
        identidade = new IdentidadeAutenticada("atendente-1", IdentidadeAutenticada.TipoIdentidade.USUARIO);
        orcamento = org.mockito.Mockito.mock(Orcamento.class);
        response = new OrcamentoResponse(null, null, null, null, List.of(), null, null, null, null, null, null);
        when(identidadeAtual.obter()).thenReturn(identidade);
    }

    @Test
    void buscaOrcamentoPorOsComIdentidadeAtual() {
        when(service.buscarPorOs("os-1", identidade)).thenReturn(orcamento);
        when(mapper.paraResponse(orcamento)).thenReturn(response);

        assertSame(response, controller.buscarPorOs("os-1"));
    }

    @Test
    void registraRespostaComIdentidadeAutenticada() {
        RespostaOrcamentoRequest request = new RespostaOrcamentoRequest(true, "aprovado");
        when(service.responder("orc-1", true, "aprovado", identidade)).thenReturn(orcamento);
        when(mapper.paraResponse(orcamento)).thenReturn(response);

        assertSame(response, controller.responder("orc-1", request));
        verify(service).responder("orc-1", true, "aprovado", identidade);
    }
}
