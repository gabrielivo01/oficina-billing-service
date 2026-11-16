package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrcamentoMapperTest {

    @Test
    void converteSnapshotCompletoDoOrcamento() {
        var orcamento = new Orcamento(
                "os-1",
                "saga-1",
                new ClienteSnapshot("cliente-1", "Maria", "52998224725", "11999999999", "maria@example.com"),
                new VeiculoSnapshot("ABC1D23", "Fiat", "Uno", 2020),
                List.of(new ItemOrcamento(
                        "SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"))),
                new BigDecimal("20.00"));

        OrcamentoResponse response = new OrcamentoMapper().paraResponse(orcamento);

        assertEquals("os-1", response.osId());
        assertEquals("maria@example.com", response.cliente().email());
        assertEquals("Uno", response.veiculo().modelo());
        assertEquals("Revisão", response.itens().getFirst().descricao());
        assertEquals(new BigDecimal("20.00"), response.valorTotal());
    }
}
