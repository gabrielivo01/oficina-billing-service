package io.github.gabrielivo.oficina.billing.infrastructure.payment;

public class GatewayPagamentoException extends RuntimeException {

    public GatewayPagamentoException(String message) {
        super(message);
    }

    public GatewayPagamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
