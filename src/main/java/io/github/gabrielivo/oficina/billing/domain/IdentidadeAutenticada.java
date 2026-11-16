package io.github.gabrielivo.oficina.billing.domain;

public record IdentidadeAutenticada(String identificador, TipoIdentidade tipo) {

    public enum TipoIdentidade {
        CLIENTE,
        USUARIO
    }
}
