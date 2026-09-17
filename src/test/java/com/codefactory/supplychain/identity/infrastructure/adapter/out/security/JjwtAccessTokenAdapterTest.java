package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtAccessTokenAdapterTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-bytes-de-largo";

    @Test
    void generaUnJwtValidoConLosClaimsEsperados() {
        JjwtAccessTokenAdapter adapter = new JjwtAccessTokenAdapter(SECRETO, 15);
        Usuario usuario = Usuario.reconstruir(java.util.UUID.randomUUID(), Email.de("ana@ejemplo.com"),
                "Ana Pérez", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv"), EstadoUsuario.ACTIVO,
                0, null, false, null, null, Instant.now(), Instant.now());

        String token = adapter.generar(usuario);

        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ana@ejemplo.com");
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
    }

    @Test
    void unTokenFirmadoConOtroSecretoEsRechazado() {
        JjwtAccessTokenAdapter adapter = new JjwtAccessTokenAdapter(SECRETO, 15);
        Usuario usuario = Usuario.reconstruir(java.util.UUID.randomUUID(), Email.de("ana@ejemplo.com"),
                "Ana Pérez", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv"), EstadoUsuario.ACTIVO,
                0, null, false, null, null, Instant.now(), Instant.now());
        String token = adapter.generar(usuario);

        // Misma longitud que SECRETO (para que jjwt elija el mismo algoritmo HMAC
        // según el tamaño de la clave) pero contenido distinto, así la falla es
        // realmente de firma inválida y no de "clave demasiado corta".
        SecretKey claveIncorrecta = Keys.hmacShaKeyFor("xx-secreto-de-prueba-de-al-menos-32-bytes-de-largo".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> Jwts.parser().verifyWith(claveIncorrecta).build().parseSignedClaims(token))
                .isInstanceOf(SignatureException.class);
    }
}
