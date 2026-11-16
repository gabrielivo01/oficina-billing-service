package io.github.gabrielivo.oficina.billing.presentation.cobranca;

import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import org.springframework.stereotype.Component;

@Component
public class CobrancaMapper {

    public CobrancaResponse paraResponse(Cobranca cobranca) {
        return new CobrancaResponse(
                cobranca.getId(),
                cobranca.getOsId(),
                cobranca.getOrcamentoId(),
                cobranca.getValor(),
                cobranca.getStatus(),
                cobranca.getLinkPagamento(),
                cobranca.getExpiraEm(),
                cobranca.getPagamentoIdExterno(),
                cobranca.getFormaPagamento());
    }
}
