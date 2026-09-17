package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class JjwtAccessTokenAdapter implements AccessTokenGeneratorPort {

    private final SecretKey clave;
    private final Duration ttl;

    public JjwtAccessTokenAdapter(
            @Value("${app.security.jwt.secret}") String secreto,
            @Value("${app.security.jwt.access-token-ttl-minutos}") long ttlMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(ttlMinutos);
    }

    @Override
    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail().getValor())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(ttl)))
                .signWith(clave)
                .compact();
    }
}
