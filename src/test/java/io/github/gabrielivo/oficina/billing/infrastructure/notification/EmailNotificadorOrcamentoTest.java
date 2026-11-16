package io.github.gabrielivo.oficina.billing.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmail;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmailRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotificadorOrcamentoTest {

    @Mock
    private NotificacaoEmailRepository repository;

    @InjectMocks
    private EmailNotificadorOrcamento notificador;

    @Test
    void gravaNotificacaoNaOutboxSemEnviarDuranteATransacao() {
        var item = new ItemOrcamento("SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"));
        var orcamento = new Orcamento(
                "os-1",
                "saga-1",
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com"),
                new VeiculoSnapshot("ABC1D23", "Fiat", "Uno", 2020),
                List.of(item),
                new BigDecimal("20.00"));

        notificador.notificar(orcamento);

        var captor = ArgumentCaptor.forClass(NotificacaoEmail.class);
        verify(repository).save(captor.capture());
        assertEquals("maria@example.com", captor.getValue().getDestinatario());
        assertEquals("Orçamento da ordem de serviço os-1", captor.getValue().getAssunto());
    }
}
