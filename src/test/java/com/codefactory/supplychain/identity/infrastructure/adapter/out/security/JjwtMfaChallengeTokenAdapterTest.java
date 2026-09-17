package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.shared.security.JwtClaimTypes;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtMfaChallengeTokenAdapterTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-bytes-de-largo";

    private final JjwtMfaChallengeTokenAdapter adapter = new JjwtMfaChallengeTokenAdapter(SECRETO, 5);

    @Test
    void generaUnTokenQueValidaAlMismoUsuarioId() {
        UUID usuarioId = UUID.randomUUID();

        String token = adapter.generar(usuarioId);

        assertThat(adapter.validar(token)).isEqualTo(usuarioId);
    }

    @Test
    void rechazaUnTokenExpirado() {
        JjwtMfaChallengeTokenAdapter adapterConTtlNegativo = new JjwtMfaChallengeTokenAdapter(SECRETO, -1);
        String tokenExpirado = adapterConTtlNegativo.generar(UUID.randomUUID());

        assertThatThrownBy(() -> adapter.validar(tokenExpirado))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "xx-secreto-de-prueba-de-al-menos-32-bytes-de-largo".getBytes(StandardCharsets.UTF_8));
        String tokenAjeno = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim(JwtClaimTypes.CLAIM_TIPO, JwtClaimTypes.TIPO_MFA_CHALLENGE)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(otraClave)
                .compact();

        assertThatThrownBy(() -> adapter.validar(tokenAjeno))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaUnAccessTokenNormalPorqueNoTieneElTipoMfaChallenge() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        String accessToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim(JwtClaimTypes.CLAIM_TIPO, JwtClaimTypes.TIPO_ACCESO)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(clave)
                .compact();

        assertThatThrownBy(() -> adapter.validar(accessToken))
                .isInstanceOf(TokenInvalidoException.class);
    }
}
