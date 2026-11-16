package io.github.gabrielivo.oficina.billing.infrastructure.payment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPDefaultHttpClient;
import com.mercadopago.net.MPHttpClient;
import com.mercadopago.net.MPRequest;
import com.mercadopago.net.MPResponse;
import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.domain.cobranca.StatusPagamentoGateway;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Adapter do Mercado Pago contra um WireMock na JVM (Etapa 4.4): o SDK oficial monta a requisição HTTP real (URL,
 * cabeçalhos, JSON) e desserializa a resposta. Nenhuma chamada sai para o Mercado Pago. O SDK sempre monta URLs com
 * {@code https://api.mercadopago.com} e o cliente HTTP dele só aceita HTTPS: o {@link Redirecionador} troca o prefixo pelo
 * endereço HTTPS do WireMock e usa um HttpClient que confia no certificado autoassinado dele (só neste teste).
 */
class GatewayPagamentoMercadoPagoWireMockTest {

    private static final String TOKEN = "TEST-0000000000000000-000000-token-de-teste";

    @RegisterExtension
    static WireMockExtension mercadoPago = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build();

    private MPHttpClient httpClientOriginal;
    private String tokenOriginal;
    private GatewayPagamentoMercadoPago gateway;

    @BeforeEach
    void setUp() {
        httpClientOriginal = MercadoPagoConfig.getHttpClient();
        tokenOriginal = MercadoPagoConfig.getAccessToken();
        MercadoPagoConfig.setAccessToken(TOKEN);
        MercadoPagoConfig.setHttpClient(
                new Redirecionador(mercadoPago.getRuntimeInfo().getHttpsBaseUrl()));

        var mensageria = new BillingProperties.Mensageria(
                "oficina.comandos",
                "oficina.eventos",
                "billing.comandos",
                new BillingProperties.Outbox(true, 20, Duration.ofSeconds(5)));
        var pagamento = new BillingProperties.Pagamento(
                "mercadopago",
                new BillingProperties.Fake("PENDENTE"),
                new BillingProperties.MercadoPago(
                        TOKEN, "segredo", "https://oficina.test/webhooks/mercadopago", "https://oficina.test/retorno"));
        gateway = new GatewayPagamentoMercadoPago(
                new PreferenceClient(),
                new PaymentClient(),
                new BillingProperties(mensageria, pagamento, new BillingProperties.Notificacao("oficina@test")));
    }

    @AfterEach
    void restaurarConfiguracaoGlobalDoSdk() {
        MercadoPagoConfig.setHttpClient(httpClientOriginal);
        MercadoPagoConfig.setAccessToken(tokenOriginal);
    }

    @Test
    void deveCriarPreferenciaComReferenciaDaOsIdempotenciaEExpiracao() {
        mercadoPago.stubFor(post(urlPathEqualTo("/checkout/preferences")).willReturn(json(201, """
                        {"id": "pref-123", "init_point": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-123"}
                        """)));
        Instant expiraEm = Instant.parse("2026-11-16T13:00:00Z");

        var resultado = gateway.criarCobranca(
                "os-1",
                "orc-1",
                "OS os-1 - Revisão",
                new BigDecimal("590.50"),
                expiraEm,
                List.of(new ItemOrcamento(
                        "PECA", "Amortecedor", "peca-1", 2, new BigDecimal("295.25"), new BigDecimal("590.50"))),
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com"));

        assertThat(resultado.idExterno()).isEqualTo("pref-123");
        assertThat(resultado.linkPagamento()).endsWith("pref_id=pref-123");
        assertThat(resultado.expiraEm()).isEqualTo(expiraEm);
        mercadoPago.verify(postRequestedFor(urlPathEqualTo("/checkout/preferences"))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN))
                .withHeader("X-Idempotency-Key", equalTo("os-1:orc-1"))
                .withRequestBody(matchingJsonPath("$.external_reference", equalTo("os-1")))
                .withRequestBody(
                        matchingJsonPath("$.notification_url", equalTo("https://oficina.test/webhooks/mercadopago")))
                .withRequestBody(matchingJsonPath("$.expires", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.items[0].id", equalTo("peca-1")))
                .withRequestBody(matchingJsonPath("$.items[0].quantity", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.payer.email", equalTo("maria@example.com"))));
    }

    @Test
    void consultaPorReferenciaDevePreferirOPagamentoAprovado() {
        mercadoPago.stubFor(get(urlPathEqualTo("/v1/payments/search"))
                .withQueryParam("external_reference", equalTo("os-1"))
                .willReturn(json(200, """
                        {"paging": {"total": 2, "limit": 50, "offset": 0},
                         "results": [
                           {"id": 1, "status": "pending", "external_reference": "os-1"},
                           {"id": 2, "status": "approved", "status_detail": "accredited", "external_reference": "os-1",
                            "transaction_amount": 590.50, "payment_type_id": "bank_transfer"}
                         ]}
                        """)));

        var resultado = gateway.consultarPagamento("os-1", "pref-123");

        assertThat(resultado.status()).isEqualTo(StatusPagamentoGateway.APROVADO);
        assertThat(resultado.pagamentoId()).isEqualTo("2");
        assertThat(resultado.valor()).isEqualByComparingTo("590.50");
    }

    @Test
    void consultaPorReferenciaSemPagamentoDeveFicarPendente() {
        mercadoPago.stubFor(get(urlPathEqualTo("/v1/payments/search")).willReturn(json(200, """
                        {"paging": {"total": 0, "limit": 50, "offset": 0}, "results": []}
                        """)));

        assertThat(gateway.consultarPagamento("os-1", "pref-123").status()).isEqualTo(StatusPagamentoGateway.PENDENTE);
    }

    @Test
    void consultaDoPagamentoNotificadoDeveMapearRejeicao() {
        mercadoPago.stubFor(get(urlPathEqualTo("/v1/payments/98765432101")).willReturn(json(200, """
                        {"id": 98765432101, "status": "rejected", "status_detail": "cc_rejected_insufficient_amount",
                         "external_reference": "os-1", "transaction_amount": 590.50, "payment_type_id": "credit_card"}
                        """)));

        var resultado = gateway.consultarPagamentoPorId("98765432101");

        assertThat(resultado.status()).isEqualTo(StatusPagamentoGateway.REJEITADO);
        assertThat(resultado.osId()).isEqualTo("os-1");
    }

    @Test
    void cancelamentoDeveCancelarPagamentosPendentesEExpirarAPreferencia() {
        mercadoPago.stubFor(get(urlPathEqualTo("/v1/payments/search")).willReturn(json(200, """
                        {"paging": {"total": 1, "limit": 50, "offset": 0},
                         "results": [{"id": 9, "status": "pending", "external_reference": "os-1"}]}
                        """)));
        mercadoPago.stubFor(put(urlPathEqualTo("/v1/payments/9")).willReturn(json(200, """
                        {"id": 9, "status": "cancelled", "external_reference": "os-1"}
                        """)));
        mercadoPago.stubFor(
                put(urlPathEqualTo("/checkout/preferences/pref-123")).willReturn(json(200, """
                        {"id": "pref-123", "expires": true}
                        """)));

        gateway.cancelar("os-1", "pref-123");

        mercadoPago.verify(putRequestedFor(urlPathEqualTo("/v1/payments/9"))
                .withRequestBody(matchingJsonPath("$.status", equalTo("cancelled"))));
        mercadoPago.verify(putRequestedFor(urlPathEqualTo("/checkout/preferences/pref-123"))
                .withRequestBody(matchingJsonPath("$.expires", equalTo("true"))));
    }

    @Test
    void estornoDeveUsarChaveDeIdempotenciaEDevolverOIdDoReembolso() {
        mercadoPago.stubFor(
                post(urlPathEqualTo("/v1/payments/98765432101/refunds")).willReturn(json(201, """
                        {"id": 55512345678, "payment_id": 98765432101, "amount": 590.50, "status": "approved"}
                        """)));

        String estornoId = gateway.estornar("os-1", "pref-123", "98765432101");

        assertThat(estornoId).isEqualTo("55512345678");
        mercadoPago.verify(postRequestedFor(urlPathEqualTo("/v1/payments/98765432101/refunds"))
                .withHeader("X-Idempotency-Key", equalTo("refund:os-1")));
    }

    @Test
    void erroDaApiDeveVirarGatewayPagamentoException() {
        mercadoPago.stubFor(post(urlPathEqualTo("/checkout/preferences")).willReturn(json(400, """
                        {"message": "invalid unit_price", "error": "bad_request", "status": 400, "cause": []}
                        """)));

        assertThatThrownBy(() -> gateway.criarCobranca(
                        "os-1",
                        "orc-1",
                        "OS",
                        BigDecimal.ONE,
                        Instant.parse("2026-11-16T13:00:00Z"),
                        List.of(new ItemOrcamento("SERVICO", "Revisão", null, 1, BigDecimal.ONE, BigDecimal.ONE)),
                        new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com")))
                .isInstanceOf(GatewayPagamentoException.class)
                .hasCauseInstanceOf(MPApiException.class);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String corpo) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(corpo);
    }

    private static HttpClient clienteQueConfiaNoWireMock() {
        try {
            SSLContext ssl = SSLContexts.custom()
                    .loadTrustMaterial(null, (cadeia, tipo) -> true)
                    .build();
            return HttpClients.custom()
                    .setSSLContext(ssl)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Envia ao WireMock as requisições que o SDK montou para a API pública do Mercado Pago. */
    private static final class Redirecionador implements MPHttpClient {

        private final String baseUrl;
        private final MPHttpClient delegado = new MPDefaultHttpClient(clienteQueConfiaNoWireMock()) {};

        private Redirecionador(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public MPResponse send(MPRequest request) throws MPException, MPApiException {
            return delegado.send(MPRequest.builder()
                    .uri(request.getUri().replace(MercadoPagoConfig.BASE_URL, baseUrl))
                    .method(request.getMethod())
                    .headers(request.getHeaders())
                    .payload(request.getPayload())
                    .queryParams(request.getQueryParams())
                    .accessToken(request.getAccessToken())
                    .connectionTimeout(request.getConnectionTimeout())
                    .connectionRequestTimeout(request.getConnectionRequestTimeout())
                    .socketTimeout(request.getSocketTimeout())
                    .build());
        }
    }
}
