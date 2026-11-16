package io.github.gabrielivo.oficina.billing.infrastructure.security;

import io.github.gabrielivo.oficina.billing.application.IdentidadeAtual;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityIdentidadeAtual implements IdentidadeAtual {

    private final SecurityProperties properties;

    public SpringSecurityIdentidadeAtual(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public IdentidadeAutenticada obter() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof IdentidadeAutenticada identity) {
            return identity;
        }
        if (!properties.enabled()) {
            return new IdentidadeAutenticada("local-staff", TipoIdentidade.USUARIO);
        }
        throw new SecurityException("Identidade autenticada não encontrada.");
    }
}
