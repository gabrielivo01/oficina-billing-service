package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import io.github.gabrielivo.oficina.billing.application.pagamento.SimuladorPagamento;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "fake", matchIfMissing = true)
public class GatewayPagamentoFake implements GatewayPagamento, SimuladorPagamento {

    private final BillingProperties properties;
    private final ConcurrentMap<String, PagamentoFake> pagamentos = new ConcurrentHashMap<>();

    public GatewayPagamentoFake(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResultadoPreferenciaGateway criarCobranca(
            String osId,
            String orcamentoId,
            String descricao,
            BigDecimal valor,
            Instant expiraEm,
            List<ItemOrcamento> itens,
            ClienteSnapshot cliente) {
        String id = "fake-" + UUID.nameUUIDFromBytes((osId + ":" + orcamentoId).getBytes(StandardCharsets.UTF_8));
        StatusPagamentoGateway initial =
                StatusPagamentoGateway.valueOf(properties.pagamento().fake().statusInicial());
        PagamentoFake pagamento = pagamentos.computeIfAbsent(
                osId, key -> new PagamentoFake(id, "fake-payment-" + id, valor, initial, null));
        return new ResultadoPreferenciaGateway(
                pagamento.idExterno(),
                "http://localhost:8082/cobrancas/" + osId + "/simular-pagamento",
                expiraEm,
                pagamento.status() == StatusPagamentoGateway.PENDENTE ? null : resultado(osId, pagamento));
    }

    @Override
    public ResultadoPagamentoGateway consultarPagamento(String osId, String idExterno) {
        PagamentoFake pagamento = exigirPagamento(osId, idExterno);
        return resultado(osId, pagamento);
    }

    @Override
    public ResultadoPagamentoGateway consultarPagamentoPorId(String pagamentoId) {
        throw new IllegalStateException("A consulta por webhook só está disponível com Mercado Pago.");
    }

    @Override
    public void cancelar(String osId, String idExterno) {
        PagamentoFake pagamento = exigirPagamento(osId, idExterno);
        pagamentos.put(
                osId,
                new PagamentoFake(
                        pagamento.idExterno(),
                        pagamento.pagamentoId(),
                        pagamento.valor(),
                        StatusPagamentoGateway.REJEITADO,
                        "Cobrança cancelada"));
    }

    @Override
    public String estornar(String osId, String idExterno, String pagamentoId) {
        PagamentoFake pagamento = exigirPagamento(osId, idExterno);
        if (!pagamento.pagamentoId().equals(pagamentoId)) {
            throw new IllegalArgumentException("Pagamento externo não corresponde à cobrança fake.");
        }
        if (pagamento.status() != StatusPagamentoGateway.APROVADO) {
            throw new IllegalStateException("Somente pagamento fake aprovado pode ser estornado.");
        }
        String refundId =
                pagamento.estornoId() == null ? "fake-refund-" + pagamento.pagamentoId() : pagamento.estornoId();
        pagamentos.put(
                osId,
                new PagamentoFake(
                        pagamento.idExterno(),
                        pagamento.pagamentoId(),
                        pagamento.valor(),
                        pagamento.status(),
                        refundId));
        return refundId;
    }

    @Override
    public void definirStatus(String osId, StatusPagamentoGateway status) {
        if (osId == null || osId.isBlank() || status == null) {
            throw new IllegalArgumentException("OS e status são obrigatórios para simular pagamento.");
        }
        PagamentoFake pagamento = pagamentos.get(osId);
        if (pagamento == null) {
            throw new IllegalArgumentException("Nenhuma cobrança fake encontrada para a OS.");
        }
        pagamentos.put(
                osId,
                new PagamentoFake(
                        pagamento.idExterno(),
                        pagamento.pagamentoId(),
                        pagamento.valor(),
                        status,
                        pagamento.estornoId()));
    }

    private PagamentoFake exigirPagamento(String osId, String idExterno) {
        if (osId == null || osId.isBlank() || idExterno == null || idExterno.isBlank()) {
            throw new IllegalArgumentException("OS e identificador externo são obrigatórios.");
        }
        PagamentoFake pagamento = pagamentos.get(osId);
        if (pagamento == null || !pagamento.idExterno().equals(idExterno)) {
            throw new IllegalArgumentException("Cobrança fake não encontrada para a OS.");
        }
        return pagamento;
    }

    private ResultadoPagamentoGateway resultado(String osId, PagamentoFake pagamento) {
        return new ResultadoPagamentoGateway(
                osId,
                pagamento.pagamentoId(),
                pagamento.status(),
                pagamento.valor(),
                "FAKE",
                pagamento.status() == StatusPagamentoGateway.REJEITADO ? "Pagamento recusado na simulação" : null);
    }

    private record PagamentoFake(
            String idExterno, String pagamentoId, BigDecimal valor, StatusPagamentoGateway status, String estornoId) {}
}
