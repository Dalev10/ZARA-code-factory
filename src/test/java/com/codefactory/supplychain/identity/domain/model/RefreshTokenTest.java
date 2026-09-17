package com.codefactory.supplychain.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void crearAsignaIdYConservaLosDatos() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(3600);

        RefreshToken token = RefreshToken.crear(usuarioId, "hash-de-prueba", ahora, expira);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(token.getTokenHash()).isEqualTo("hash-de-prueba");
        assertThat(token.getCreadoEn()).isEqualTo(ahora);
        assertThat(token.getExpiraEn()).isEqualTo(expira);
    }

    @Test
    void dosTokensSonIgualesSoloSiTienenElMismoId() {
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        RefreshToken token1 = RefreshToken.reconstruir(id, UUID.randomUUID(), "hash-1", ahora, ahora);
        RefreshToken token2 = RefreshToken.reconstruir(id, UUID.randomUUID(), "hash-2", ahora, ahora);

        assertThat(token1).isEqualTo(token2);
    }

    @Test
    void reconstruirPreservaElEstadoPersistido() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant expira = Instant.parse("2026-01-08T00:00:00Z");

        RefreshToken token = RefreshToken.reconstruir(id, usuarioId, "hash-persistido", creado, expira);

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(token.getTokenHash()).isEqualTo("hash-persistido");
        assertThat(token.getCreadoEn()).isEqualTo(creado);
        assertThat(token.getExpiraEn()).isEqualTo(expira);
    }
}
