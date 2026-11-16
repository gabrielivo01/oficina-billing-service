package io.github.gabrielivo.oficina.billing.application.orcamento;

public class OrcamentoNaoEncontradoException extends RuntimeException {

    public OrcamentoNaoEncontradoException(String id) {
        super("Orçamento não encontrado: " + id);
    }
}
