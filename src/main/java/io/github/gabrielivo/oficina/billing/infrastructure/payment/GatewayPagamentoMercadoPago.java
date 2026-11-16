package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pagamento.gateway", havingValue = "mercadopago")
public class GatewayPagamentoMercadoPago implements GatewayPagamento {

    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;
    private final BillingProperties properties;

    public GatewayPagamentoMercadoPago(
            PreferenceClient preferenceClient, PaymentClient paymentClient, BillingProperties properties) {
        this.preferenceClient = preferenceClient;
        this.paymentClient = paymentClient;
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
        var requestItems = itens.stream()
                .map(item -> PreferenceItemRequest.builder()
                        .id(item.pecaId())
                        .title(item.descricao())
                        .quantity(item.quantidade())
                        .currencyId("BRL")
                        .unitPrice(item.valorUnitario())
                        .build())
                .toList();
        var request = PreferenceRequest.builder()
                .items(requestItems)
                .payer(PreferencePayerRequest.builder()
                        .name(cliente.nome())
                        .email(cliente.email())
                        .build())
                .externalReference(osId)
                .notificationUrl(properties.pagamento().mercadopago().notificationUrl())
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(properties.pagamento().mercadopago().backUrl())
                        .pending(properties.pagamento().mercadopago().backUrl())
                        .failure(properties.pagamento().mercadopago().backUrl())
                        .build())
                .expires(true)
                .expirationDateTo(OffsetDateTime.ofInstant(expiraEm, ZoneOffset.UTC))
                .build();
        try {
            var preference = preferenceClient.create(
                    request,
                    MPRequestOptions.builder()
                            .customHeaders(Map.of("X-Idempotency-Key", osId + ":" + orcamentoId))
                            .build());
            return new ResultadoPreferenciaGateway(preference.getId(), preference.getInitPoint(), expiraEm);
        } catch (MPException | MPApiException e) {
            throw new GatewayPagamentoException("Não foi possível criar a preferência no Mercado Pago.", e);
        }
    }

    @Override
    public ResultadoPagamentoGateway consultarPagamento(String osId, String idExterno) {
        try {
            var search = paymentClient.search(MPSearchRequest.builder()
                    .limit(50)
                    .offset(0)
                    .filters(Map.of(
                            "external_reference", osId,
                            "sort", "date_created",
                            "criteria", "desc"))
                    .build());
            var payments = search.getResults();
            if (payments == null) {
                throw new GatewayPagamentoException("O Mercado Pago retornou uma consulta sem lista de pagamentos.");
            }
            Payment payment = payments.stream()
                    .filter(candidate -> osId.equals(candidate.getExternalReference()))
                    .filter(candidate -> "approved".equals(candidate.getStatus()))
                    .findFirst()
                    .orElseGet(() -> payments.stream()
                            .filter(candidate -> osId.equals(candidate.getExternalReference()))
                            .findFirst()
                            .orElse(null));
            return payment == null
                    ? new ResultadoPagamentoGateway(osId, null, StatusPagamentoGateway.PENDENTE, null, null, null)
                    : mapear(payment);
        } catch (MPException | MPApiException e) {
            throw new GatewayPagamentoException("Não foi possível consultar pagamentos no Mercado Pago.", e);
        }
    }

    @Override
    public ResultadoPagamentoGateway consultarPagamentoPorId(String pagamentoId) {
        try {
            return mapear(paymentClient.get(Long.valueOf(pagamentoId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identificador de pagamento inválido.", e);
        } catch (MPException | MPApiException e) {
            throw new GatewayPagamentoException("Não foi possível consultar o pagamento notificado.", e);
        }
    }

    @Override
    public void cancelar(String osId, String idExterno) {
        try {
            var pagamentos = paymentClient.search(MPSearchRequest.builder()
                    .limit(50)
                    .offset(0)
                    .filters(Map.of("external_reference", osId, "status", "pending"))
                    .build());
            for (Payment payment : pagamentos.getResults()) {
                if (payment.getId() != null
                        && osId.equals(payment.getExternalReference())
                        && "pending".equals(payment.getStatus())) {
                    paymentClient.cancel(payment.getId());
                }
            }
            preferenceClient.update(
                    idExterno,
                    PreferenceRequest.builder()
                            .expires(true)
                            .expirationDateTo(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1))
                            .build());
        } catch (MPException | MPApiException e) {
            throw new GatewayPagamentoException("Não foi possível cancelar a cobrança no Mercado Pago.", e);
        }
    }

    @Override
    public String estornar(String osId, String idExterno, String pagamentoId) {
        if (pagamentoId == null || pagamentoId.isBlank()) {
            throw new IllegalStateException("A cobrança aprovada não possui pagamento externo para estornar.");
        }
        try {
            var refund = paymentClient.refund(
                    Long.valueOf(pagamentoId),
                    MPRequestOptions.builder()
                            .customHeaders(Map.of("X-Idempotency-Key", "refund:" + osId))
                            .build());
            if (refund == null || refund.getId() == null) {
                throw new GatewayPagamentoException("Mercado Pago não retornou o identificador do estorno.");
            }
            return refund.getId().toString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identificador de pagamento inválido para estorno.", e);
        } catch (MPException | MPApiException e) {
            throw new GatewayPagamentoException("Não foi possível estornar o pagamento no Mercado Pago.", e);
        }
    }

    private ResultadoPagamentoGateway mapear(Payment payment) {
        StatusPagamentoGateway status = switch (payment.getStatus() == null ? "" : payment.getStatus()) {
            case "approved" -> StatusPagamentoGateway.APROVADO;
            case "rejected", "cancelled", "refunded", "charged_back" -> StatusPagamentoGateway.REJEITADO;
            default -> StatusPagamentoGateway.PENDENTE;
        };
        return new ResultadoPagamentoGateway(
                payment.getExternalReference(),
                payment.getId() == null ? null : payment.getId().toString(),
                status,
                payment.getTransactionAmount(),
                payment.getPaymentTypeId() == null ? payment.getPaymentMethodId() : payment.getPaymentTypeId(),
                payment.getStatusDetail());
    }
}
