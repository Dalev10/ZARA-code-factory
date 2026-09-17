package com.codefactory.supplychain.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-bytes-de-largo";

    private final ScopesUsuarioPort scopesUsuarioPort = mock(ScopesUsuarioPort.class);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(SECRETO, scopesUsuarioPort);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unTokenValidoDejaAlUsuarioAutenticadoEnElContexto() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String token = generarToken(usuarioId, Duration.ofMinutes(15));
        when(scopesUsuarioPort.obtenerScopes(usuarioId)).thenReturn(List.of());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filtro.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(usuarioId);
        verify(chain).doFilter(request, response);
    }

    @Test
    void unTokenValidoPueblaLasAutoridadesConLosScopesDelUsuario() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String token = generarToken(usuarioId, Duration.ofMinutes(15));
        when(scopesUsuarioPort.obtenerScopes(usuarioId)).thenReturn(List.of("usuarios:administrar", "roles:administrar"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filtro.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("usuarios:administrar", "roles:administrar");
    }

    @Test
    void sinHeaderAuthorizationLaPeticionSigueAnonima() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        filtro.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void unTokenExpiradoNoAutenticaYLaPeticionContinuaAnonima() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String tokenExpirado = generarToken(usuarioId, Duration.ofMinutes(-15));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenExpirado);

        filtro.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void unTokenDeDesafioMfaNoSirveComoBearerAccessToken() throws Exception {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();
        String tokenDeDesafio = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim(JwtClaimTypes.CLAIM_TIPO, JwtClaimTypes.TIPO_MFA_CHALLENGE)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(300)))
                .signWith(clave)
                .compact();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenDeDesafio);

        filtro.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void unTokenFirmadoConOtroSecretoNoAutentica() throws Exception {
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "xx-secreto-de-prueba-de-al-menos-32-bytes-de-largo".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(otraClave)
                .compact();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filtro.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    private static String generarToken(UUID usuarioId, Duration validezDesdeAhora) {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim(JwtClaimTypes.CLAIM_TIPO, JwtClaimTypes.TIPO_ACCESO)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(validezDesdeAhora)))
                .signWith(clave)
                .compact();
    }
}
