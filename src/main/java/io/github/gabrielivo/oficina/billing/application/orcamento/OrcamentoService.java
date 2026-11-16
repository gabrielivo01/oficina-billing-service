package io.github.gabrielivo.oficina.billing.application.orcamento;

import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.OrcamentoRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrcamentoService {

    private final OrcamentoRepository repository;
    private final OutboxService outboxService;
    private final NotificadorOrcamento notificador;

    public OrcamentoService(
            OrcamentoRepository repository, OutboxService outboxService, NotificadorOrcamento notificador) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.notificador = notificador;
    }

    @Transactional
    public Orcamento gerar(GerarOrcamentoCommand command) {
        if (command == null || blank(command.osId()) || blank(command.sagaId())) {
            throw new IllegalArgumentException("osId e sagaId são obrigatórios para gerar orçamento.");
        }
        var existing = repository.findByOsId(command.osId());
        if (existing.isPresent()) {
            return existing.get();
        }
        Orcamento orcamento = repository.save(new Orcamento(
                command.osId(),
                command.sagaId(),
                command.cliente(),
                command.veiculo(),
                command.itens(),
                command.valorTotal()));
        notificador.notificar(orcamento);
        outboxService.registrar(
                "OrcamentoGerado",
                orcamento.getSagaId(),
                orcamento.getOsId(),
                Map.of(
                        "orcamentoId",
                        orcamento.getId(),
                        "valorTotal",
                        orcamento.getValorTotal(),
                        "geradoEm",
                        orcamento.getCriadoEm().toString()));
        return orcamento;
    }

    @Transactional(readOnly = true)
    public Orcamento buscarPorOs(String osId, IdentidadeAutenticada identidade) {
        Orcamento orcamento = repository.findByOsId(osId).orElseThrow(() -> new OrcamentoNaoEncontradoException(osId));
        validarAcesso(orcamento, identidade);
        return orcamento;
    }

    @Transactional
    public Orcamento responder(String id, boolean aprovado, String observacao, IdentidadeAutenticada identidade) {
        Orcamento orcamento = repository.findById(id).orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
        validarAcesso(orcamento, identidade);
        boolean alterado = orcamento.responder(aprovado, observacao, identidade);
        if (alterado) {
            repository.save(orcamento);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orcamentoId", orcamento.getId());
            payload.put("valorTotal", orcamento.getValorTotal());
            payload.put(
                    "respondidoPor",
                    Map.of(
                            "tipo", orcamento.getRespondidoPorTipo(),
                            "identificador", orcamento.getRespondidoPorIdentificador()));
            payload.put("observacao", orcamento.getObservacao());
            payload.put("respondidoEm", orcamento.getRespondidoEm().toString());
            String type = aprovado ? "OrcamentoAprovado" : "OrcamentoRecusado";
            outboxService.registrar(type, orcamento.getSagaId(), orcamento.getOsId(), payload);
        }
        return orcamento;
    }

    @Transactional(readOnly = true)
    public Orcamento buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
    }

    private void validarAcesso(Orcamento orcamento, IdentidadeAutenticada identidade) {
        if (identidade == null || identidade.tipo() == null) {
            throw new SecurityException("Identidade autenticada ausente.");
        }
        if (identidade.tipo() == IdentidadeAutenticada.TipoIdentidade.CLIENTE
                && !orcamento.getCliente().cpf().equals(identidade.identificador())) {
            throw new SecurityException("O cliente autenticado não é titular deste orçamento.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
