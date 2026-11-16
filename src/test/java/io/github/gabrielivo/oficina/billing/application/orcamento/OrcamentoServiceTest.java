package io.github.gabrielivo.oficina.billing.application.orcamento;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.mensageria.OutboxService;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.OrcamentoRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private NotificadorOrcamento notificador;

    @InjectMocks
    private OrcamentoService service;

    @Test
    void gerarEResponderPublicaUmaVezMesmoComReentrega() {
        var command = new GerarOrcamentoCommand(
                "os-1",
                "saga-1",
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, "maria@example.com"),
                new VeiculoSnapshot("ABC1D23", "Fiat", "Uno", 2020),
                List.of(new ItemOrcamento(
                        "SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"))),
                new BigDecimal("20.00"));
        when(repository.findByOsId("os-1")).thenReturn(Optional.empty());
        when(repository.save(any(Orcamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Orcamento criado = service.gerar(command);
        when(repository.findByOsId("os-1")).thenReturn(Optional.of(criado));
        assertSame(criado, service.gerar(command));
        when(repository.findById(criado.getId())).thenReturn(Optional.of(criado));
        verify(notificador).notificar(criado);
        verify(outboxService).registrar(eq("OrcamentoGerado"), eq("saga-1"), eq("os-1"), any());

        var atendente = new IdentidadeAutenticada("atendente-1", IdentidadeAutenticada.TipoIdentidade.USUARIO);
        service.responder(criado.getId(), true, "Aprovado", atendente);
        service.responder(criado.getId(), true, "Aprovado novamente", atendente);
        verify(repository, times(2)).save(criado);
        verify(outboxService).registrar(eq("OrcamentoAprovado"), eq("saga-1"), eq("os-1"), any());
        verify(outboxService, times(2)).registrar(any(), any(), any(), any());
    }
}
