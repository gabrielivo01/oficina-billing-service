package io.github.gabrielivo.oficina.billing.infrastructure.security;

import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties properties;

    public JwtAuthenticationFilter(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();
                String subject = claims.getSubject();
                String tipo = claims.get("tipo", String.class);
                var identity = new IdentidadeAutenticada(
                        subject, tipo == null ? TipoIdentidade.USUARIO : TipoIdentidade.valueOf(tipo));
                var authentication = new UsernamePasswordAuthenticationToken(identity, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT inválido ou expirado.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
