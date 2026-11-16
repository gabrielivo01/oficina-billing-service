package io.github.gabrielivo.oficina.billing.presentation.cobranca;

import io.github.gabrielivo.oficina.billing.application.IdentidadeAtual;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cobrancas")
public class CobrancaController {

    private final CobrancaService service;
    private final CobrancaMapper mapper;
    private final IdentidadeAtual identidadeAtual;

    public CobrancaController(CobrancaService service, CobrancaMapper mapper, IdentidadeAtual identidadeAtual) {
        this.service = service;
        this.mapper = mapper;
        this.identidadeAtual = identidadeAtual;
    }

    @GetMapping("/{osId}")
    public CobrancaResponse buscar(@PathVariable String osId) {
        return mapper.paraResponse(service.buscarPorOs(osId, identidadeAtual.obter()));
    }
}
