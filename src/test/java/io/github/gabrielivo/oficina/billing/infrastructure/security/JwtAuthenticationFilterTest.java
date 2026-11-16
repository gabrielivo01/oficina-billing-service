package io.github.gabrielivo.oficina.billing.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-256-bits-para-hs256-ok";

    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(new JwtProperties(SEGREDO));

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenDeStaffSemClaimTipoDeveAutenticarComoUsuario() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(
                requisicao("Bearer " + token(SEGREDO, "atendente", Map.of())), new MockHttpServletResponse(), cadeia);

        assertThat(cadeia.getRequest()).isNotNull();
        assertThat(identidadeAutenticada()).isEqualTo(new IdentidadeAutenticada("atendente", TipoIdentidade.USUARIO));
    }

    @Test
    void tokenDeClienteDeveAutenticarComoCliente() throws Exception {
        filtro.doFilter(
                requisicao("Bearer " + token(SEGREDO, "52998224725", Map.of("tipo", "CLIENTE"))),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(identidadeAutenticada()).isEqualTo(new IdentidadeAutenticada("52998224725", TipoIdentidade.CLIENTE));
    }

    @Test
    void requisicaoSemTokenDeveSeguirSemAutenticacao() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), resposta, cadeia);

        assertThat(cadeia.getRequest()).isNotNull();
        assertThat(resposta.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void cabecalhoQueNaoEBearerDeveSerIgnorado() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(requisicao("Basic dXN1YXJpbzpzZW5oYQ=="), new MockHttpServletResponse(), cadeia);

        assertThat(cadeia.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenAssinadoComOutroSegredoDeveResponder401SemSeguirACadeia() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        String outroSegredo = "outro-segredo-com-pelo-menos-256-bits-para-assinatura-hs256";

        filtro.doFilter(requisicao("Bearer " + token(outroSegredo, "atendente", Map.of())), resposta, cadeia);

        assertThat(resposta.getStatus()).isEqualTo(401);
        assertThat(cadeia.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenExpiradoDeveResponder401() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        String expirado = Jwts.builder()
                .subject("atendente")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        filtro.doFilter(requisicao("Bearer " + expirado), resposta, new MockFilterChain());

        assertThat(resposta.getStatus()).isEqualTo(401);
    }

    @Test
    void claimTipoDesconhecidaDeveResponder401() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(
                requisicao("Bearer " + token(SEGREDO, "x", Map.of("tipo", "ADMIN"))), resposta, new MockFilterChain());

        assertThat(resposta.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest requisicao(String authorization) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/orcamentos/os-1");
        requisicao.addHeader("Authorization", authorization);
        return requisicao;
    }

    private static String token(String segredo, String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static Object identidadeAutenticada() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
