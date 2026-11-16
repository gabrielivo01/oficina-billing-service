package io.github.gabrielivo.oficina.billing.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityIdentidadeAtualTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveDevolverAIdentidadeColocadaPeloFiltroJwt() {
        var identidade = new IdentidadeAutenticada("52998224725", TipoIdentidade.CLIENTE);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(identidade, null, List.of()));

        assertThat(new SpringSecurityIdentidadeAtual(new SecurityProperties(true)).obter())
                .isEqualTo(identidade);
    }

    @Test
    void comSegurancaDesligadaDeveUsarStaffLocal() {
        assertThat(new SpringSecurityIdentidadeAtual(new SecurityProperties(false)).obter())
                .isEqualTo(new IdentidadeAutenticada("local-staff", TipoIdentidade.USUARIO));
    }

    @Test
    void comSegurancaLigadaEPrincipalDeOutroTipoDeveRecusar() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonimo", null, List.of()));

        assertThatThrownBy(() -> new SpringSecurityIdentidadeAtual(new SecurityProperties(true)).obter())
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void comSegurancaLigadaESemAutenticacaoDeveRecusar() {
        assertThatThrownBy(() -> new SpringSecurityIdentidadeAtual(new SecurityProperties(true)).obter())
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Identidade autenticada");
    }
}
