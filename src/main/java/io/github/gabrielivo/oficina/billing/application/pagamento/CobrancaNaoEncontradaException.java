package io.github.gabrielivo.oficina.billing.application.pagamento;

public class CobrancaNaoEncontradaException extends RuntimeException {

    public CobrancaNaoEncontradaException(String id) {
        super("Cobrança não encontrada: " + id);
    }
}
