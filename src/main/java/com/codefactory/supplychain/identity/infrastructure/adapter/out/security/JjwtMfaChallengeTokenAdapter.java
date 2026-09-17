package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.MfaChallengeTokenPort;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.shared.security.JwtClaimTypes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class JjwtMfaChallengeTokenAdapter implements MfaChallengeTokenPort {

    private final SecretKey clave;
    private final Duration ttl;

    public JjwtMfaChallengeTokenAdapter(
            @Value("${app.security.jwt.secret}") String secreto,
            @Value("${app.security.jwt.mfa-challenge-ttl-minutos}") long ttlMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(ttlMinutos);
    }

    @Override
    public String generar(UUID usuarioId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim(JwtClaimTypes.CLAIM_TIPO, JwtClaimTypes.TIPO_MFA_CHALLENGE)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(ttl)))
                .signWith(clave)
                .compact();
    }

    @Override
    public UUID validar(String challengeToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(challengeToken).getPayload();
            if (!JwtClaimTypes.TIPO_MFA_CHALLENGE.equals(claims.get(JwtClaimTypes.CLAIM_TIPO, String.class))) {
                throw new TokenInvalidoException();
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException excepcionTokenInvalido) {
            throw new TokenInvalidoException();
        }
    }
}
