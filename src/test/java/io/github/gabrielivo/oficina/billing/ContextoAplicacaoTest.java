package io.github.gabrielivo.oficina.billing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gabrielivo.oficina.billing.application.pagamento.GatewayPagamento;
import io.github.gabrielivo.oficina.billing.infrastructure.config.MongoIndexInitializer;
import io.github.gabrielivo.oficina.billing.infrastructure.messaging.ConsumidorComandos;
import io.github.gabrielivo.oficina.billing.infrastructure.notification.PublicadorNotificacoesEmail;
import io.github.gabrielivo.oficina.billing.infrastructure.payment.GatewayPagamentoFake;
import io.github.gabrielivo.oficina.billing.infrastructure.payment.VerificadorCobrancasPendentes;
import io.github.gabrielivo.oficina.billing.presentation.orcamento.OrcamentoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Garante que a aplicação monta sem erro de wiring, sem MongoDB, RabbitMQ nem SMTP: o cliente Mongo é criado mas não
 * usado (o inicializador de índices e os jobs que consultam o banco são substituídos por mocks), os listeners AMQP não
 * iniciam e o publicador do outbox fica desligado. Segurança ligada e gateway fake, como no perfil padrão.
 */
@SpringBootTest(
        properties = {
            "spring.mongodb.uri=mongodb://localhost:1/billing_test?serverSelectionTimeoutMS=200&connectTimeoutMS=200",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "app.mensageria.outbox.publicador-habilitado=false",
            "management.tracing.export.otlp.enabled=false",
            "management.tracing.sampling.probability=0.0",
            "jwt.secret=segredo-de-teste-com-pelo-menos-256-bits-para-hs256-ok"
        })
class ContextoAplicacaoTest {

    @MockitoBean
    private MongoIndexInitializer mongoIndexInitializer;

    @MockitoBean
    private VerificadorCobrancasPendentes verificadorCobrancasPendentes;

    @MockitoBean
    private PublicadorNotificacoesEmail publicadorNotificacoesEmail;

    @Autowired
    private ApplicationContext contexto;

    @Test
    void contextoDeveCarregarComGatewayFakePorPadrao() {
        assertThat(contexto.getBean(GatewayPagamento.class)).isInstanceOf(GatewayPagamentoFake.class);
        assertThat(contexto.getBean(OrcamentoController.class)).isNotNull();
        assertThat(contexto.getBean(ConsumidorComandos.class)).isNotNull();
    }

    /** Regressão: no Spring Boot 4 a URI vem de {@code spring.mongodb.uri}; a antiga {@code spring.data.*} é ignorada. */
    @Test
    void uriDoMongoDeveVirDaPropriedadeDoSpringBoot4() {
        assertThat(contexto.getBean(MongoConnectionDetails.class)
                        .getConnectionString()
                        .getHosts())
                .containsExactly("localhost:1");
    }
}
