package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.net.MPResultsResourcesPage;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GatewayPagamentoMercadoPagoTest {

    @Mock
    private PreferenceClient preferenceClient;

    @Mock
    private PaymentClient paymentClient;

    private GatewayPagamentoMercadoPago gateway;

    @BeforeEach
    void setUp() {
        var mensageria = new BillingProperties.Mensageria(
                "commands",
                "events",
                "billing.commands",
                new BillingProperties.Outbox(true, 20, Duration.ofSeconds(5)));
        var pagamento = new BillingProperties.Pagamento(
                "mercadopago",
                new BillingProperties.Fake("PENDENTE"),
                new BillingProperties.MercadoPago("token", "secret", "https://notify.test", "https://back.test"));
        gateway = new GatewayPagamentoMercadoPago(
                preferenceClient,
                paymentClient,
                new BillingProperties(mensageria, pagamento, new BillingProperties.Notificacao("from")));
    }

    @Test
    void criaPreferenciaComExpiracaoEDevolveLinkDePagamento() throws Exception {
        Preference preference = mock(Preference.class);
        when(preferenceClient.create(any(PreferenceRequest.class), any(MPRequestOptions.class)))
                .thenReturn(preference);
        when(preference.getId()).thenReturn("pref-1");
        when(preference.getInitPoint()).thenReturn("https://pay.test/pref-1");
        Instant expiry = Instant.parse("2026-11-16T13:00:00Z");

        var result = gateway.criarCobranca(
                "os-1",
                "orc-1",
                "Serviço",
                new BigDecimal("20.00"),
                expiry,
                List.of(new ItemOrcamento(
                        "SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"))),
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com"));

        assertEquals("pref-1", result.idExterno());
        assertEquals("https://pay.test/pref-1", result.linkPagamento());
        assertEquals(expiry, result.expiraEm());
    }

    @Test
    void consultaPreferePagamentoAprovadoEMapeiaStatus() throws Exception {
        Payment pending = payment("os-1", "pending");
        Payment approved = payment("os-1", "approved");
        when(approved.getId()).thenReturn(2L);
        MPResultsResourcesPage<Payment> results = new MPResultsResourcesPage<>();
        results.setResults(List.of(pending, approved));
        when(paymentClient.search(any(MPSearchRequest.class))).thenReturn(results);

        var result = gateway.consultarPagamento("os-1", "pref-1");

        assertEquals(StatusPagamentoGateway.APROVADO, result.status());
        assertEquals("2", result.pagamentoId());
    }

    @Test
    void consultaSemPagamentoRetornaPendenteEIdInvalidoERejeitado() throws Exception {
        MPResultsResourcesPage<Payment> results = new MPResultsResourcesPage<>();
        results.setResults(List.of());
        when(paymentClient.search(any(MPSearchRequest.class))).thenReturn(results);

        assertEquals(
                StatusPagamentoGateway.PENDENTE,
                gateway.consultarPagamento("os-1", "pref-1").status());
        assertThrows(IllegalArgumentException.class, () -> gateway.consultarPagamentoPorId("not-a-number"));
    }

    @Test
    void consultaPorIdMapeiaPagamento() throws Exception {
        Payment payment = payment("os-1", "rejected");
        when(payment.getId()).thenReturn(12L);
        when(paymentClient.get(12L)).thenReturn(payment);

        var result = gateway.consultarPagamentoPorId("12");

        assertEquals(StatusPagamentoGateway.REJEITADO, result.status());
        assertEquals("12", result.pagamentoId());
    }

    @Test
    void cancelaPagamentosPendentesDaOsEExpiraPreferencia() throws Exception {
        Payment pending = payment("os-1", "pending");
        Payment approved = payment("os-1", "approved");
        Payment otherOrder = mock(Payment.class);
        when(otherOrder.getExternalReference()).thenReturn("os-2");
        when(pending.getId()).thenReturn(1L);
        when(approved.getId()).thenReturn(2L);
        when(otherOrder.getId()).thenReturn(3L);
        MPResultsResourcesPage<Payment> results = new MPResultsResourcesPage<>();
        results.setResults(List.of(pending, approved, otherOrder));
        when(paymentClient.search(any(MPSearchRequest.class))).thenReturn(results);

        gateway.cancelar("os-1", "pref-1");

        verify(paymentClient).cancel(1L);
        verify(paymentClient, org.mockito.Mockito.never()).cancel(2L);
        verify(paymentClient, org.mockito.Mockito.never()).cancel(3L);
        verify(preferenceClient).update(eq("pref-1"), any(PreferenceRequest.class));
    }

    @Test
    void estornaPagamentoComChaveIdempotenteERejeitaIdsInvalidos() throws Exception {
        PaymentRefund refund = mock(PaymentRefund.class);
        when(refund.getId()).thenReturn(123L);
        when(paymentClient.refund(eq(7L), any(MPRequestOptions.class))).thenReturn(refund);

        assertEquals("123", gateway.estornar("os-1", "pref-1", "7"));
        assertThrows(IllegalStateException.class, () -> gateway.estornar("os-1", "pref-1", null));
        assertThrows(IllegalArgumentException.class, () -> gateway.estornar("os-1", "pref-1", "invalid"));
    }

    private Payment payment(String osId, String status) {
        Payment payment = mock(Payment.class);
        when(payment.getExternalReference()).thenReturn(osId);
        when(payment.getStatus()).thenReturn(status);
        return payment;
    }
}
