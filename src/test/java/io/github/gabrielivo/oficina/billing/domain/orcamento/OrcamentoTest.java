package io.github.gabrielivo.oficina.billing.domain.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrcamentoTest {

    @Test
    void exigeEmailETotalConsistente() {
        assertThrows(IllegalArgumentException.class, () -> novoOrcamento(null));
        assertThrows(IllegalArgumentException.class, () -> {
            var item = item();
            new Orcamento("os-1", "saga-1", cliente(), veiculo(), List.of(item), new BigDecimal("19.99"));
        });

        assertEquals(StatusOrcamento.PENDENTE, novoOrcamento(cliente()).getStatus());
    }

    @Test
    void respostaDoTitularEIdempotenteMasNaoPodeSerAlterada() {
        var orcamento = novoOrcamento(cliente());
        var titular = new IdentidadeAutenticada("52998224725", IdentidadeAutenticada.TipoIdentidade.CLIENTE);

        assertTrue(orcamento.responder(true, "Aprovado", titular));
        assertFalse(orcamento.responder(true, "Aprovado novamente", titular));
        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
        assertEquals("Aprovado", orcamento.getObservacao());
        assertThrows(IllegalStateException.class, () -> orcamento.responder(false, null, titular));
        assertThrows(
                SecurityException.class,
                () -> orcamento.responder(
                        true,
                        null,
                        new IdentidadeAutenticada("11111111111", IdentidadeAutenticada.TipoIdentidade.CLIENTE)));
    }

    private Orcamento novoOrcamento(ClienteSnapshot cliente) {
        return new Orcamento("os-1", "saga-1", cliente, veiculo(), List.of(item()), new BigDecimal("20.00"));
    }

    private ClienteSnapshot cliente() {
        return cliente("maria@example.com");
    }

    private ClienteSnapshot cliente(String email) {
        return new ClienteSnapshot("cliente-1", "Maria", "52998224725", null, email);
    }

    private VeiculoSnapshot veiculo() {
        return new VeiculoSnapshot("ABC1D23", "Fiat", "Uno", 2020);
    }

    private ItemOrcamento item() {
        return new ItemOrcamento("SERVICO", "Revisão", null, 1, new BigDecimal("20.00"), new BigDecimal("20.00"));
    }
}
