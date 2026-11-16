package io.github.gabrielivo.oficina.billing.application.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import io.github.gabrielivo.oficina.billing.domain.cobranca.CobrancaRepository;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusCobranca;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.OrcamentoRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CobrancaServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private GatewayPagamento gateway;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CobrancaService service;

    @Test
    void criaCobrancaDoOrcamentoAprovadoEPublicaConfirmacaoUmaVez() {
        Orcamento orcamento = orcamentoAprovado();
        Instant prazo = Instant.now().plusSeconds(600);
        when(cobrancaRepository.findByOsId("os-1")).thenReturn(Optional.empty());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(gateway.criarCobranca(
                        eq("os-1"),
                        eq(orcamento.getId()),
                        eq("Revisão"),
                        eq(new BigDecimal("20.00")),
                        eq(prazo),
                        any(),
                        any()))
                .thenReturn(new ResultadoPreferenciaGateway("pref-1", "https://example.test/pay", prazo));
        when(cobrancaRepository.save(any(Cobranca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cobranca cobranca =
                service.criar(new CriarCobrancaCommand("os-1", "saga-1", orcamento.getId(), "Revisão", prazo));
        when(cobrancaRepository.findByOsId("os-1")).thenReturn(Optional.of(cobranca));
        var pagamento = new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "pix", null);

        service.atualizarPagamento(pagamento);
        service.atualizarPagamento(pagamento);

        assertEquals(StatusCobranca.APROVADA, cobranca.getStatus());
        verify(outboxService).registrar(eq("CobrancaCriada"), eq("saga-1"), eq("os-1"), any());
        verify(outboxService).registrar(eq("PagamentoConfirmado"), eq("os-1"), eq("os-1"), any());
        verify(outboxService, times(2)).registrar(any(), any(), any(), any());
        verify(gateway, never()).estornar(any(), any(), any());
    }

    @Test
    void publicaPagamentoInicialAprovadoPeloGatewayFake() {
        Orcamento orcamento = orcamentoAprovado();
        Instant prazo = Instant.now().plusSeconds(600);
        AtomicReference<Cobranca> salva = new AtomicReference<>();
        when(cobrancaRepository.findByOsId("os-1")).thenAnswer(invocation -> Optional.ofNullable(salva.get()));
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(gateway.criarCobranca(
                        eq("os-1"),
                        eq(orcamento.getId()),
                        eq("Revisão"),
                        eq(new BigDecimal("20.00")),
                        eq(prazo),
                        any(),
                        any()))
                .thenReturn(new ResultadoPreferenciaGateway(
                        "pref-1",
                        "http://localhost:8082/cobrancas/os-1/simular-pagamento",
                        prazo,
                        new ResultadoPagamentoGateway(
                                "os-1",
                                "fake-payment",
                                StatusPagamentoGateway.APROVADO,
                                new BigDecimal("20.00"),
                                "FAKE",
                                null)));
        when(cobrancaRepository.save(any(Cobranca.class))).thenAnswer(invocation -> {
            Cobranca cobranca = invocation.getArgument(0);
            salva.set(cobranca);
            return cobranca;
        });

        Cobranca cobranca =
                service.criar(new CriarCobrancaCommand("os-1", "saga-1", orcamento.getId(), "Revisão", prazo));

        assertEquals(StatusCobranca.APROVADA, cobranca.getStatus());
        verify(outboxService).registrar(eq("CobrancaCriada"), eq("saga-1"), eq("os-1"), any());
        verify(outboxService).registrar(eq("PagamentoConfirmado"), eq("os-1"), eq("os-1"), any());
    }

    @Test
    void cancelarCobrancaPendentePublicaConfirmacaoDepoisDoGateway() {
        Instant prazo = Instant.now().plusSeconds(600);
        Cobranca cobranca = new Cobranca(
                "os-1",
                "orcamento-1",
                new BigDecimal("20.00"),
                new ResultadoPreferenciaGateway("pref-1", "https://example.test/pay", prazo),
                prazo);
        when(cobrancaRepository.findByOsId("os-1")).thenReturn(Optional.of(cobranca));

        service.cancelar("os-1", cobranca.getId(), "PRAZO_PAGAMENTO_EXPIRADO");

        assertEquals(StatusCobranca.CANCELADA, cobranca.getStatus());
        verify(gateway).cancelar("os-1", "pref-1");
        verify(outboxService).registrar(eq("CobrancaCancelada"), eq("os-1"), eq("os-1"), any());
    }

    @Test
    void sePagamentoJaFoiAprovadoCancelamentoSolicitaEstornoNaSaga() {
        Instant prazo = Instant.now().plusSeconds(600);
        Cobranca cobranca = new Cobranca(
                "os-1",
                "orcamento-1",
                new BigDecimal("20.00"),
                new ResultadoPreferenciaGateway("pref-1", "https://example.test/pay", prazo),
                prazo);
        cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-1", "pag-1", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "PIX", null));
        when(cobrancaRepository.findByOsId("os-1")).thenReturn(Optional.of(cobranca));

        service.cancelar("os-1", cobranca.getId(), "CANCELAMENTO");

        verify(gateway, never()).cancelar(any(), any());
        verify(outboxService).registrar(eq("PagamentoConfirmado"), eq("os-1"), eq("os-1"), any());
        verify(outboxService, never()).registrar(eq("CobrancaCancelada"), any(), any(), any());
    }

    @Test
    void criarRejeitaEntradaInvalidaEReutilizaCobrancaDoMesmoOrcamento() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(new CriarCobrancaCommand(" ", "saga-1", "orc-1", "Serviço", Instant.now())));
        verify(cobrancaRepository, never()).findByOsId(any());

        Cobranca existente = cobrancaPendente("os-2", "orc-2");
        when(cobrancaRepository.findByOsId("os-2")).thenReturn(Optional.of(existente));

        assertSame(
                existente,
                service.criar(new CriarCobrancaCommand(
                        "os-2", "saga-2", "orc-2", "Serviço", Instant.now().plusSeconds(600))));
        assertThrows(
                IllegalStateException.class,
                () -> service.criar(new CriarCobrancaCommand(
                        "os-2",
                        "saga-2",
                        "outro-orcamento",
                        "Serviço",
                        Instant.now().plusSeconds(600))));
        verifyNoInteractions(orcamentoRepository, gateway, outboxService);
    }

    @Test
    void atualizarPagamentoRejeitadoPublicaMotivoDoGateway() {
        Cobranca cobranca = cobrancaPendente("os-3", "orc-3");
        when(cobrancaRepository.findByOsId("os-3")).thenReturn(Optional.of(cobranca));

        service.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-3", "pag-3", StatusPagamentoGateway.REJEITADO, null, null, "Cartão recusado"));

        assertEquals(StatusCobranca.REJEITADA, cobranca.getStatus());
        verify(cobrancaRepository).save(cobranca);
        verify(outboxService)
                .registrar(
                        eq("PagamentoRejeitado"),
                        eq("os-3"),
                        eq("os-3"),
                        argThat(payload -> payload.get("motivo").equals("Cartão recusado")));
    }

    @Test
    void verificaPagamentoPendenteEListaIdsAguardando() {
        Cobranca cobranca = cobrancaPendente("os-4", "orc-4");
        when(cobrancaRepository.findByOsId("os-4")).thenReturn(Optional.of(cobranca));
        when(cobrancaRepository.findTop100ByStatusOrderByCriadaEmAsc(StatusCobranca.PENDENTE))
                .thenReturn(List.of(cobranca));
        when(gateway.consultarPagamento("os-4", "pref-os-4"))
                .thenReturn(new ResultadoPagamentoGateway(
                        "os-4", "pag-4", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "PIX", null));

        assertEquals(List.of("os-4"), service.listarIdsPendentes());
        service.verificarPagamentoPendente("os-4");

        assertEquals(StatusCobranca.APROVADA, cobranca.getStatus());
        verify(gateway).consultarPagamento("os-4", "pref-os-4");
        verify(outboxService).registrar(eq("PagamentoConfirmado"), eq("os-4"), eq("os-4"), any());
    }

    @Test
    void estornaCobrancaAprovadaETrataRepeticaoComSeguranca() {
        Cobranca cobranca = cobrancaPendente("os-5", "orc-5");
        cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-5", "pag-5", StatusPagamentoGateway.APROVADO, new BigDecimal("20.00"), "PIX", null));
        when(cobrancaRepository.findByOsId("os-5")).thenReturn(Optional.of(cobranca));
        when(gateway.estornar("os-5", "pref-os-5", "pag-5")).thenReturn("refund-5");

        service.estornar("os-5", cobranca.getId());
        service.estornar("os-5", cobranca.getId());

        assertEquals(StatusCobranca.ESTORNADA, cobranca.getStatus());
        assertEquals("refund-5", cobranca.getEstornoIdExterno());
        verify(gateway, times(1)).estornar("os-5", "pref-os-5", "pag-5");
        verify(outboxService).registrar(eq("PagamentoEstornado"), eq("os-5"), eq("os-5"), any());
    }

    @Test
    void acessoAComprovanteDeCobrancaERestritoAoClienteDoOrcamento() {
        Cobranca cobranca = cobrancaPendente("os-6", "orc-6");
        when(cobrancaRepository.findByOsId("os-6")).thenReturn(Optional.of(cobranca));
        Orcamento orcamento = orcamentoAprovado();
        when(orcamentoRepository.findById("orc-6")).thenReturn(Optional.of(orcamento));

        assertSame(
                cobranca,
                service.buscarPorOs(
                        "os-6",
                        new io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada(
                                "52998224725",
                                io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade
                                        .CLIENTE)));
        assertThrows(
                SecurityException.class,
                () -> service.buscarPorOs(
                        "os-6",
                        new io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada(
                                "11111111111",
                                io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade
                                        .CLIENTE)));
        assertThrows(SecurityException.class, () -> service.buscarPorOs("os-6", null));
    }

    @Test
    void cancelamentoInformaIdentificadorIncorretoEEstadoNaoCancelavel() {
        Cobranca cobranca = cobrancaPendente("os-7", "orc-7");
        cobranca.atualizarPagamento(new ResultadoPagamentoGateway(
                "os-7", "pag-7", StatusPagamentoGateway.REJEITADO, null, null, "recusado"));
        when(cobrancaRepository.findByOsId("os-7")).thenReturn(Optional.of(cobranca));

        assertThrows(IllegalArgumentException.class, () -> service.cancelar("os-7", "outra-cobranca", "motivo"));
        service.cancelar("os-7", cobranca.getId(), null);

        verify(gateway, never()).cancelar(any(), any());
        verify(outboxService)
                .registrar(
                        eq("CobrancaCancelada"),
                        eq("os-7"),
                        eq("os-7"),
                        argThat(payload -> payload.get("motivo").equals("CANCELAMENTO")));
    }

    @Test
    void rejeitaAtualizacaoDePagamentoSemIdentidadeDaOrdem() {
        assertThrows(IllegalArgumentException.class, () -> service.atualizarPagamento(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizarPagamento(new ResultadoPagamentoGateway(
                        " ", "pag-1", StatusPagamentoGateway.PENDENTE, null, null, null)));
        verifyNoInteractions(cobrancaRepository, orcamentoRepository, gateway, outboxService);
    }

    private Orcamento orcamentoAprovado() {
        var orcamento = new Orcamento(
                "os-1",
                "saga-1",
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com"),
                new VeiculoSnapshot("ABC1D23", "Fiat", "Uno", 2020),
                List.of(new ItemOrcamento(
                        "SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"))),
                new BigDecimal("20.00"));
        orcamento.responder(
                true,
                null,
                new io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada(
                        "atendente-1",
                        io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade.USUARIO));
        return orcamento;
    }

    private Cobranca cobrancaPendente(String osId, String orcamentoId) {
        Instant prazo = Instant.now().plusSeconds(600);
        return new Cobranca(
                osId,
                orcamentoId,
                new BigDecimal("20.00"),
                new ResultadoPreferenciaGateway("pref-" + osId, "https://example.test/pay", prazo),
                prazo);
    }
}
