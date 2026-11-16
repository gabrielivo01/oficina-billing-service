package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import io.github.gabrielivo.oficina.billing.application.IdentidadeAtual;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoService;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private final OrcamentoService service;
    private final OrcamentoMapper mapper;
    private final IdentidadeAtual identidadeAtual;

    public OrcamentoController(OrcamentoService service, OrcamentoMapper mapper, IdentidadeAtual identidadeAtual) {
        this.service = service;
        this.mapper = mapper;
        this.identidadeAtual = identidadeAtual;
    }

    @GetMapping("/{osId}")
    public OrcamentoResponse buscarPorOs(@PathVariable String osId) {
        return mapper.paraResponse(service.buscarPorOs(osId, identidadeAtual.obter()));
    }

    @PostMapping("/{id}/resposta")
    public OrcamentoResponse responder(@PathVariable String id, @Valid @RequestBody RespostaOrcamentoRequest request) {
        IdentidadeAutenticada identidade = identidadeAtual.obter();
        return mapper.paraResponse(service.responder(id, request.aprovado(), request.observacao(), identidade));
    }
}
