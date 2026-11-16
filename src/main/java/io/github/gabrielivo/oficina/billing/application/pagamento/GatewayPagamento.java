package io.github.gabrielivo.oficina.billing.application.pagamento;

import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface GatewayPagamento {

    ResultadoPreferenciaGateway criarCobranca(
            String osId,
            String orcamentoId,
            String descricao,
            BigDecimal valor,
            Instant expiraEm,
            List<ItemOrcamento> itens,
            ClienteSnapshot cliente);

    ResultadoPagamentoGateway consultarPagamento(String osId, String idExterno);

    ResultadoPagamentoGateway consultarPagamentoPorId(String pagamentoId);

    void cancelar(String osId, String idExterno);

    String estornar(String osId, String idExterno, String pagamentoId);
}
