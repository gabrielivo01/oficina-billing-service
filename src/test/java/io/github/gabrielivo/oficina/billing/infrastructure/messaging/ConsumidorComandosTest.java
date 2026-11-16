package io.github.gabrielivo.oficina.billing.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.gabrielivo.oficina.billing.application.mensageria.ProcessadorMensagensService;
import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ConsumidorComandosTest {

    private static final String COMANDO = """
            {"messageId":"m-1","type":"EstornarPagamento","sagaId":"os-1","osId":"os-1",
             "occurredAt":"2026-11-15T15:20:05Z","payload":{"cobrancaId":"c-1","motivo":"EXECUCAO_RECUSADA"}}
            """;

    @Mock
    private ProcessadorMensagensService processador;

    private ConsumidorComandos consumidor;

    @BeforeEach
    void setUp() {
        consumidor = new ConsumidorComandos(JsonMapper.builder().build(), processador);
    }

    @Test
    void deveDesserializarOEnvelopeEDelegarAoProcessador() {
        consumidor.consumir(COMANDO);

        ArgumentCaptor<EnvelopeMensagem> captor = ArgumentCaptor.forClass(EnvelopeMensagem.class);
        verify(processador).processar(captor.capture());
        assertThat(captor.getValue().messageId()).isEqualTo("m-1");
        assertThat(captor.getValue().type()).isEqualTo("EstornarPagamento");
        assertThat(captor.getValue().osId()).isEqualTo("os-1");
        assertThat(captor.getValue().payload()).containsEntry("cobrancaId", "c-1");
    }

    @Test
    void corpoQueNaoEUmEnvelopeDeveFalharSemChamarOProcessador() {
        assertThatThrownBy(() -> consumidor.consumir("{nao é json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("envelope esperado");
        verifyNoInteractions(processador);
    }

    @Test
    void falhaDoProcessadorDevePropagarParaORetryEDlqDoListener() {
        doThrow(new IllegalStateException("Mongo indisponível"))
                .when(processador)
                .processar(any());

        assertThatThrownBy(() -> consumidor.consumir(COMANDO)).isInstanceOf(IllegalStateException.class);
    }
}
