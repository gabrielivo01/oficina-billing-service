package io.github.gabrielivo.oficina.billing.presentation.cobranca;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabrielivo.oficina.billing.domain.cobranca.Cobranca;
import io.github.gabrielivo.oficina.billing.domain.cobranca.ResultadoPreferenciaGateway;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CobrancaMapperTest {

    @Test
    void converteCamposDaCobrancaParaResponse() {
        Instant expiry = Instant.now().plusSeconds(600);
        Cobranca cobranca = new Cobranca(
                "os-1",
                "orc-1",
                new BigDecimal("20.00"),
                new ResultadoPreferenciaGateway("pref-1", "https://example.test/pay", expiry),
                expiry);

        CobrancaResponse response = new CobrancaMapper().paraResponse(cobranca);

        assertEquals(cobranca.getId(), response.id());
        assertEquals("https://example.test/pay", response.linkPagamento());
        assertEquals(expiry, response.expiraEm());
    }
}
