package io.github.gabrielivo.oficina.billing.application.mensageria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.orcamento.GerarOrcamentoCommand;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CriarCobrancaCommand;
import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemProcessada;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemProcessadaRepository;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ProcessadorMensagensServiceTest {

    @Mock
    private MensagemProcessadaRepository inbox;

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private CobrancaService cobrancaService;

    @Test
    void desserializaContratoGerarOrcamentoEPropagaEmailDoCliente() throws Exception {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        var service = new ProcessadorMensagensService(inbox, orcamentoService, cobrancaService, mapper);
        when(inbox.existsById("5e7d0a10-0000-4000-8000-000000000004")).thenReturn(false);
        try (InputStream fixture = getClass().getResourceAsStream("/contratos/GerarOrcamento.json")) {
            EnvelopeMensagem envelope = mapper.readValue(fixture, EnvelopeMensagem.class);

            service.processar(envelope);
        }

        var captor = ArgumentCaptor.forClass(GerarOrcamentoCommand.class);
        verify(orcamentoService).gerar(captor.capture());
        assertEquals("maria.souza@example.com", captor.getValue().cliente().email());
        verify(inbox).save(any(MensagemProcessada.class));
        verify(cobrancaService, never()).criar(any());
    }

    @Test
    void ignoraMensagemJaProcessada() {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        var service = new ProcessadorMensagensService(inbox, orcamentoService, cobrancaService, mapper);
        when(inbox.existsById("message-1")).thenReturn(true);
        var mensagem = new EnvelopeMensagem("message-1", "GerarOrcamento", "saga-1", "os-1", null, null);

        service.processar(mensagem);

        verify(orcamentoService, never()).gerar(any());
        verify(inbox, never()).save(any());
    }

    @Test
    void desserializaContratosDeCriacaoECancelamentoDeCobranca() throws Exception {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        var service = new ProcessadorMensagensService(inbox, orcamentoService, cobrancaService, mapper);
        when(inbox.existsById("5e7d0a10-0000-4000-8000-000000000008")).thenReturn(false);
        when(inbox.existsById("5e7d0a10-0000-4000-8000-000000000018")).thenReturn(false);
        when(inbox.existsById("5e7d0a10-0000-4000-8000-000000000019")).thenReturn(false);

        service.processar(ler(mapper, "/contratos/CriarCobranca.json"));
        service.processar(ler(mapper, "/contratos/CancelarCobranca.json"));
        service.processar(ler(mapper, "/contratos/EstornarPagamento.json"));

        var captor = ArgumentCaptor.forClass(CriarCobrancaCommand.class);
        verify(cobrancaService).criar(captor.capture());
        assertEquals("b1110000-0004-4000-8000-000000000004", captor.getValue().orcamentoId());
        verify(cobrancaService)
                .cancelar(
                        "8d3b7c1e-2f4a-4e6b-9c0d-5a1b2c3d4e5f",
                        "b1110000-0005-4000-8000-000000000005",
                        "PRAZO_PAGAMENTO_EXPIRADO");
        verify(cobrancaService)
                .estornar("8d3b7c1e-2f4a-4e6b-9c0d-5a1b2c3d4e5f", "b1110000-0005-4000-8000-000000000005");
    }

    private EnvelopeMensagem ler(JsonMapper mapper, String resource) throws Exception {
        try (InputStream fixture = getClass().getResourceAsStream(resource)) {
            return mapper.readValue(fixture, EnvelopeMensagem.class);
        }
    }
}
