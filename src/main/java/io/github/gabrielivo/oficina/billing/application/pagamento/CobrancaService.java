package io.github.gabrielivo.oficina.billing.application.pagamento;

import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoNaoEncontradoException;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import io.github.gabrielivo.oficina.billing.domain.cobranca.CobrancaRepository;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusCobranca;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.OrcamentoRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.StatusOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final GatewayPagamento gateway;
    private final OutboxService outboxService;

    public CobrancaService(
            CobrancaRepository cobrancaRepository,
            OrcamentoRepository orcamentoRepository,
            GatewayPagamento gateway,
            OutboxService outboxService) {
        this.cobrancaRepository = cobrancaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.gateway = gateway;
        this.outboxService = outboxService;
    }

    @Transactional
    public Cobranca criar(CriarCobrancaCommand command) {
        if (command == null
                || blank(command.osId())
                || blank(command.sagaId())
                || blank(command.orcamentoId())
                || blank(command.descricao())) {
            throw new IllegalArgumentException("osId, sagaId, orcamentoId e descrição são obrigatórios.");
        }
        var existing = cobrancaRepository.findByOsId(command.osId());
        if (existing.isPresent()) {
            if (!existing.get().getOrcamentoId().equals(command.orcamentoId())) {
                throw new IllegalStateException("Já existe cobrança para outro orçamento desta OS.");
            }
            return existing.get();
        }
        Orcamento orcamento = orcamentoRepository
                .findById(command.orcamentoId())
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(command.orcamentoId()));
        if (!orcamento.getOsId().equals(command.osId()) || orcamento.getStatus() != StatusOrcamento.APROVADO) {
            throw new IllegalStateException("A cobrança exige o orçamento aprovado da mesma OS.");
        }
        if (command.prazoPagamento() == null || !command.prazoPagamento().isAfter(Instant.now())) {
            throw new IllegalArgumentException("O prazo de pagamento deve estar no futuro.");
        }
        BigDecimal valor = orcamento.getValorTotal();
        var preference = gateway.criarCobranca(
                command.osId(),
                orcamento.getId(),
                command.descricao(),
                valor,
                command.prazoPagamento(),
                orcamento.getItens(),
                orcamento.getCliente());
        Cobranca cobranca = cobrancaRepository.save(
                new Cobranca(command.osId(), orcamento.getId(), valor, preference, command.prazoPagamento()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cobrancaId", cobranca.getId());
        payload.put("orcamentoId", cobranca.getOrcamentoId());
        payload.put("idExterno", cobranca.getIdExterno());
        payload.put("valor", cobranca.getValor());
        payload.put("linkPagamento", cobranca.getLinkPagamento());
        payload.put("qrCode", null);
        payload.put("expiraEm", cobranca.getExpiraEm().toString());
        outboxService.registrar("CobrancaCriada", command.sagaId(), command.osId(), payload);
        if (preference.pagamentoInicial() != null) {
            atualizarPagamento(preference.pagamentoInicial());
        }
        return cobranca;
    }

    @Transactional
    public void cancelar(String osId, String cobrancaId, String motivo) {
        Cobranca cobranca = buscarPorOs(osId);
        if (!cobranca.getId().equals(cobrancaId)) {
            throw new IllegalArgumentException("A cobrança informada não corresponde à OS.");
        }
        if (cobranca.getStatus() == StatusCobranca.APROVADA) {
            publicarPagamentoConfirmado(cobranca);
            return;
        }
        if (cobranca.getStatus() == StatusCobranca.PENDENTE) {
            gateway.cancelar(osId, cobranca.getIdExterno());
            cobranca.cancelar(motivo);
            cobrancaRepository.save(cobranca);
        } else {
            cobranca.cancelar(motivo);
        }
        outboxService.registrar(
                "CobrancaCancelada",
                osId,
                osId,
                Map.of(
                        "cobrancaId", cobranca.getId(),
                        "motivo", motivo == null ? "CANCELAMENTO" : motivo,
                        "canceladaEm", Instant.now().toString()));
    }

    @Transactional
    public void estornar(String osId, String cobrancaId) {
        Cobranca cobranca = buscarPorOs(osId);
        if (!cobranca.getId().equals(cobrancaId)) {
            throw new IllegalArgumentException("A cobrança informada não corresponde à OS.");
        }
        if (cobranca.getStatus() == StatusCobranca.ESTORNADA) {
            return;
        }
        if (cobranca.getStatus() != StatusCobranca.APROVADA) {
            throw new IllegalStateException("Somente cobrança aprovada pode ser estornada.");
        }
        String refundId = gateway.estornar(osId, cobranca.getIdExterno(), cobranca.getPagamentoIdExterno());
        cobranca.estornar(refundId);
        cobrancaRepository.save(cobranca);
        outboxService.registrar(
                "PagamentoEstornado",
                osId,
                osId,
                Map.of(
                        "cobrancaId",
                        cobranca.getId(),
                        "estornoIdExterno",
                        refundId,
                        "valorEstornado",
                        cobranca.getValor(),
                        "estornadoEm",
                        cobranca.getAtualizadaEm().toString()));
    }

    @Transactional
    public void atualizarPagamento(ResultadoPagamentoGateway resultado) {
        if (resultado == null || resultado.osId() == null || resultado.osId().isBlank()) {
            throw new IllegalArgumentException("Resultado de pagamento deve identificar a OS.");
        }
        Cobranca cobranca = buscarPorOs(resultado.osId());
        if (!cobranca.getOsId().equals(resultado.osId())) {
            throw new SecurityException("O pagamento consultado não corresponde à cobrança.");
        }
        if (cobranca.atualizarPagamento(resultado)) {
            cobrancaRepository.save(cobranca);
            if (cobranca.getStatus() == StatusCobranca.APROVADA) {
                publicarPagamentoConfirmado(cobranca);
            } else if (cobranca.getStatus() == StatusCobranca.REJEITADA) {
                publicarPagamentoRejeitado(cobranca);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<String> listarIdsPendentes() {
        return cobrancaRepository.findTop100ByStatusOrderByCriadaEmAsc(StatusCobranca.PENDENTE).stream()
                .map(Cobranca::getOsId)
                .toList();
    }

    @Transactional
    public void verificarPagamentoPendente(String osId) {
        Cobranca cobranca = buscarPorOs(osId);
        if (cobranca.getStatus() == StatusCobranca.PENDENTE) {
            atualizarPagamento(gateway.consultarPagamento(osId, cobranca.getIdExterno()));
        }
    }

    @Transactional(readOnly = true)
    public Cobranca buscarPorOs(String osId) {
        return cobrancaRepository.findByOsId(osId).orElseThrow(() -> new CobrancaNaoEncontradaException(osId));
    }

    @Transactional(readOnly = true)
    public Cobranca buscarPorOs(String osId, IdentidadeAutenticada identidade) {
        Cobranca cobranca = buscarPorOs(osId);
        Orcamento orcamento = orcamentoRepository
                .findById(cobranca.getOrcamentoId())
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(cobranca.getOrcamentoId()));
        if (identidade == null
                || identidade.tipo() == null
                || (identidade.tipo() == IdentidadeAutenticada.TipoIdentidade.CLIENTE
                        && !orcamento.getCliente().cpf().equals(identidade.identificador()))) {
            throw new SecurityException("O usuário autenticado não tem acesso a esta cobrança.");
        }
        return cobranca;
    }

    private void publicarPagamentoConfirmado(Cobranca cobranca) {
        outboxService.registrar(
                "PagamentoConfirmado",
                cobranca.getOsId(),
                cobranca.getOsId(),
                Map.of(
                        "cobrancaId",
                        cobranca.getId(),
                        "pagamentoIdExterno",
                        cobranca.getPagamentoIdExterno(),
                        "valor",
                        cobranca.getValor(),
                        "formaPagamento",
                        cobranca.getFormaPagamento() == null ? "DESCONHECIDO" : cobranca.getFormaPagamento(),
                        "confirmadoEm",
                        cobranca.getAtualizadaEm().toString()));
    }

    private void publicarPagamentoRejeitado(Cobranca cobranca) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cobrancaId", cobranca.getId());
        payload.put("pagamentoIdExterno", cobranca.getPagamentoIdExterno());
        payload.put(
                "motivo",
                cobranca.getMotivo() == null || cobranca.getMotivo().isBlank()
                        ? "Pagamento rejeitado pelo gateway"
                        : cobranca.getMotivo());
        payload.put("rejeitadoEm", cobranca.getAtualizadaEm().toString());
        outboxService.registrar("PagamentoRejeitado", cobranca.getOsId(), cobranca.getOsId(), payload);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
