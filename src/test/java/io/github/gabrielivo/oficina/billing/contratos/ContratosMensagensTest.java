package io.github.gabrielivo.oficina.billing.contratos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.application.mensageria.ProcessadorMensagensService;
import io.github.gabrielivo.oficina.billing.application.orcamento.GerarOrcamentoCommand;
import io.github.gabrielivo.oficina.billing.application.orcamento.NotificadorOrcamento;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CriarCobrancaCommand;
import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import io.github.gabrielivo.oficina.billing.domain.cobranca.CobrancaRepository;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemProcessadaRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.OrcamentoRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes de contrato do Billing com o catálogo ({@code oficina/documentacao/contratos_mensagens.md}; exemplos copiados
 * em {@code src/test/resources/contratos/}).
 *
 * <ul>
 *   <li><b>Consumidos</b>: cada comando de exemplo passa pelo {@link ProcessadorMensagensService} real e precisa
 *       chegar ao caso de uso com os campos do exemplo.
 *   <li><b>Produzidos</b>: os serviços reais geram cada evento; o envelope serializado precisa ter os mesmos campos
 *       (e os mesmos tipos JSON) do exemplo do catálogo.
 * </ul>
 */
class ContratosMensagensTest {

    private static final String OS = "8d3b7c1e-2f4a-4e6b-9c0d-5a1b2c3d4e5f";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Map<String, Map<String, Object>> eventos = new HashMap<>();

    private OrcamentoRepository orcamentoRepository;
    private CobrancaRepository cobrancaRepository;
    private GatewayPagamento gateway;
    private OrcamentoService orcamentoService;
    private CobrancaService cobrancaService;

    @BeforeEach
    void setUp() {
        orcamentoRepository = mock(OrcamentoRepository.class);
        cobrancaRepository = mock(CobrancaRepository.class);
        gateway = mock(GatewayPagamento.class);
        OutboxService outbox = mock(OutboxService.class);
        org.mockito.Mockito.doAnswer(invocation -> {
                    eventos.put(invocation.getArgument(0), invocation.getArgument(3));
                    return null;
                })
                .when(outbox)
                .registrar(anyString(), anyString(), anyString(), any());
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cobrancaRepository.save(any(Cobranca.class))).thenAnswer(invocation -> invocation.getArgument(0));
        orcamentoService = new OrcamentoService(orcamentoRepository, outbox, mock(NotificadorOrcamento.class));
        cobrancaService = new CobrancaService(cobrancaRepository, orcamentoRepository, gateway, outbox);
    }

    // ---------------------------------------------------------------- comandos consumidos

    @Test
    void gerarOrcamentoDoCatalogoDeveChegarAoCasoDeUsoComClienteVeiculoEItens() throws IOException {
        OrcamentoService orcamentos = mock(OrcamentoService.class);
        processador(orcamentos, mock(CobrancaService.class)).processar(comando("GerarOrcamento"));

        ArgumentCaptor<GerarOrcamentoCommand> captor = ArgumentCaptor.forClass(GerarOrcamentoCommand.class);
        verify(orcamentos).gerar(captor.capture());
        GerarOrcamentoCommand command = captor.getValue();
        JsonNode payload = exemplo("GerarOrcamento").get("payload");
        assertThat(command.osId()).isEqualTo(OS);
        assertThat(command.cliente().email())
                .isEqualTo(payload.get("cliente").get("email").asString());
        assertThat(command.cliente().cpf())
                .isEqualTo(payload.get("cliente").get("cpf").asString());
        assertThat(command.veiculo().placa())
                .isEqualTo(payload.get("veiculo").get("placa").asString());
        assertThat(command.itens()).hasSize(payload.get("itens").size());
        assertThat(command.valorTotal())
                .isEqualByComparingTo(payload.get("valorTotal").decimalValue());
    }

    @Test
    void criarCobrancaDoCatalogoDeveChegarAoCasoDeUsoComOrcamentoEPrazo() throws IOException {
        CobrancaService cobrancas = mock(CobrancaService.class);
        processador(mock(OrcamentoService.class), cobrancas).processar(comando("CriarCobranca"));

        ArgumentCaptor<CriarCobrancaCommand> captor = ArgumentCaptor.forClass(CriarCobrancaCommand.class);
        verify(cobrancas).criar(captor.capture());
        JsonNode payload = exemplo("CriarCobranca").get("payload");
        assertThat(captor.getValue().orcamentoId())
                .isEqualTo(payload.get("orcamentoId").asString());
        assertThat(captor.getValue().descricao())
                .isEqualTo(payload.get("descricao").asString());
        assertThat(captor.getValue().prazoPagamento())
                .isEqualTo(Instant.parse(payload.get("prazoPagamento").asString()));
    }

    @Test
    void cancelarCobrancaEEstornarPagamentoDoCatalogoDevemChegarAosCasosDeUso() throws IOException {
        CobrancaService cobrancas = mock(CobrancaService.class);
        ProcessadorMensagensService processador = processador(mock(OrcamentoService.class), cobrancas);

        processador.processar(comando("CancelarCobranca"));
        processador.processar(comando("EstornarPagamento"));

        JsonNode cancelar = exemplo("CancelarCobranca").get("payload");
        verify(cobrancas)
                .cancelar(
                        OS,
                        cancelar.get("cobrancaId").asString(),
                        cancelar.get("motivo").asString());
        verify(cobrancas)
                .estornar(
                        OS,
                        exemplo("EstornarPagamento")
                                .get("payload")
                                .get("cobrancaId")
                                .asString());
    }

    // ---------------------------------------------------------------- eventos produzidos

    @Test
    void orcamentoGeradoAprovadoERecusadoDevemSeguirOCatalogo() throws IOException {
        GerarOrcamentoCommand command = processadoComo("GerarOrcamento");
        Orcamento orcamento = orcamentoService.gerar(command);
        assertSegueOContrato("OrcamentoGerado");

        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        orcamentoService.responder(orcamento.getId(), true, "Pode fazer", cliente(command));
        assertSegueOContrato("OrcamentoAprovado");

        Orcamento outro = orcamentoService.gerar(command);
        when(orcamentoRepository.findById(outro.getId())).thenReturn(Optional.of(outro));
        orcamentoService.responder(
                outro.getId(),
                false,
                "Cliente achou caro",
                new IdentidadeAutenticada("atendente", IdentidadeAutenticada.TipoIdentidade.USUARIO));
        assertSegueOContrato("OrcamentoRecusado");
    }

    @Test
    void eventosDaCobrancaDevemSeguirOCatalogo() throws IOException {
        Orcamento orcamento = orcamentoService.gerar(processadoComo("GerarOrcamento"));
        orcamento.responder(
                true, null, new IdentidadeAutenticada("atendente", IdentidadeAutenticada.TipoIdentidade.USUARIO));
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        Instant prazo = Instant.now().plusSeconds(1800);
        when(gateway.criarCobranca(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoPreferenciaGateway("pref-1", "https://sandbox.test/pref-1", prazo));
        when(cobrancaRepository.findByOsId(OS)).thenReturn(Optional.empty());

        Cobranca cobranca =
                cobrancaService.criar(new CriarCobrancaCommand(OS, OS, orcamento.getId(), "OS - Revisão", prazo));
        assertSegueOContrato("CobrancaCriada");

        when(cobrancaRepository.findByOsId(OS)).thenReturn(Optional.of(cobranca));
        cobrancaService.atualizarPagamento(new ResultadoPagamentoGateway(
                OS, "98765432101", StatusPagamentoGateway.APROVADO, cobranca.getValor(), "PIX", null));
        assertSegueOContrato("PagamentoConfirmado");

        when(gateway.estornar(any(), any(), any())).thenReturn("55512345678");
        cobrancaService.estornar(OS, cobranca.getId());
        assertSegueOContrato("PagamentoEstornado");
    }

    @Test
    void pagamentoRejeitadoECobrancaCanceladaDevemSeguirOCatalogo() throws IOException {
        Instant prazo = Instant.now().plusSeconds(1800);
        Cobranca rejeitada = new Cobranca(
                OS,
                "orcamento-1",
                new BigDecimal("590.50"),
                new ResultadoPreferenciaGateway("pref-1", "https://sandbox.test/pref-1", prazo),
                prazo);
        when(cobrancaRepository.findByOsId(OS)).thenReturn(Optional.of(rejeitada));
        cobrancaService.atualizarPagamento(new ResultadoPagamentoGateway(
                OS,
                "98765432101",
                StatusPagamentoGateway.REJEITADO,
                new BigDecimal("590.50"),
                "credit_card",
                "cc_rejected_insufficient_amount"));
        assertSegueOContrato("PagamentoRejeitado");

        Cobranca pendente = new Cobranca(
                OS,
                "orcamento-2",
                new BigDecimal("590.50"),
                new ResultadoPreferenciaGateway("pref-2", "https://sandbox.test/pref-2", prazo),
                prazo);
        when(cobrancaRepository.findByOsId(OS)).thenReturn(Optional.of(pendente));
        cobrancaService.cancelar(OS, pendente.getId(), "PRAZO_PAGAMENTO_EXPIRADO");
        assertSegueOContrato("CobrancaCancelada");
    }

    // ---------------------------------------------------------------- apoio

    /**
     * O envelope serializado do evento produzido tem exatamente os campos do exemplo, com o mesmo tipo JSON. Campos
     * opcionais listados podem faltar no evento produzido.
     */
    private void assertSegueOContrato(String tipo, String... opcionais) throws IOException {
        Map<String, Object> payload = eventos.get(tipo);
        assertThat(payload).as("evento %s não foi registrado no outbox", tipo).isNotNull();
        JsonNode produzido = jsonMapper.readTree(
                jsonMapper.writeValueAsString(new EnvelopeMensagem("m-1", tipo, OS, OS, Instant.now(), payload)));
        JsonNode esperado = exemplo(tipo);

        assertThat(nomes(produzido)).as("campos do envelope de %s", tipo).isEqualTo(nomes(esperado));
        Set<String> esperados = nomes(esperado.get("payload"));
        Set<String> obrigatorios = new HashSet<>(esperados);
        obrigatorios.removeAll(Set.of(opcionais));
        assertThat(nomes(produzido.get("payload")))
                .as("campos do payload de %s", tipo)
                .containsAll(obrigatorios)
                .isSubsetOf(esperados);
        for (String campo : nomes(produzido.get("payload"))) {
            JsonNode valorEsperado = esperado.get("payload").get(campo);
            JsonNode valorProduzido = produzido.get("payload").get(campo);
            if (!valorEsperado.isNull() && !valorProduzido.isNull()) {
                assertThat(valorProduzido.getNodeType())
                        .as("tipo JSON de %s.%s", tipo, campo)
                        .isEqualTo(valorEsperado.getNodeType());
            }
        }
    }

    private GerarOrcamentoCommand processadoComo(String tipo) throws IOException {
        OrcamentoService orcamentos = mock(OrcamentoService.class);
        processador(orcamentos, mock(CobrancaService.class)).processar(comando(tipo));
        ArgumentCaptor<GerarOrcamentoCommand> captor = ArgumentCaptor.forClass(GerarOrcamentoCommand.class);
        verify(orcamentos).gerar(captor.capture());
        return captor.getValue();
    }

    private static IdentidadeAutenticada cliente(GerarOrcamentoCommand command) {
        return new IdentidadeAutenticada(command.cliente().cpf(), IdentidadeAutenticada.TipoIdentidade.CLIENTE);
    }

    private ProcessadorMensagensService processador(OrcamentoService orcamentos, CobrancaService cobrancas) {
        return new ProcessadorMensagensService(
                mock(MensagemProcessadaRepository.class), orcamentos, cobrancas, jsonMapper);
    }

    private EnvelopeMensagem comando(String tipo) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/contratos/" + tipo + ".json")) {
            return jsonMapper.readValue(in, EnvelopeMensagem.class);
        }
    }

    private JsonNode exemplo(String tipo) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/contratos/" + tipo + ".json")) {
            return jsonMapper.readTree(in);
        }
    }

    private static Set<String> nomes(JsonNode no) {
        Set<String> nomes = new HashSet<>();
        no.propertyNames().forEach(nomes::add);
        return nomes;
    }
}
