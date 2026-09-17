package com.codefactory.supplychain.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoRespaldoTest {

    @Test
    void crearAsignaIdYNoQuedaUsado() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();

        CodigoRespaldo codigo = CodigoRespaldo.crear(usuarioId, "hash-de-prueba", ahora);

        assertThat(codigo.getId()).isNotNull();
        assertThat(codigo.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(codigo.getCodigoHash()).isEqualTo("hash-de-prueba");
        assertThat(codigo.getUsadoEn()).isNull();
        assertThat(codigo.estaUsado()).isFalse();
    }

    @Test
    void marcarUsadoFijaLaFechaDeUso() {
        CodigoRespaldo codigo = CodigoRespaldo.crear(UUID.randomUUID(), "hash", Instant.now());
        Instant ahora = Instant.now();

        CodigoRespaldo usado = codigo.marcarUsado(ahora);

        assertThat(usado.getUsadoEn()).isEqualTo(ahora);
        assertThat(usado.estaUsado()).isTrue();
    }

    @Test
    void reconstruirPreservaElEstadoPersistido() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant usado = Instant.parse("2026-01-02T00:00:00Z");

        CodigoRespaldo codigo = CodigoRespaldo.reconstruir(id, usuarioId, "hash-persistido", creado, usado);

        assertThat(codigo.getId()).isEqualTo(id);
        assertThat(codigo.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(codigo.getCodigoHash()).isEqualTo("hash-persistido");
        assertThat(codigo.getCreadoEn()).isEqualTo(creado);
        assertThat(codigo.getUsadoEn()).isEqualTo(usado);
        assertThat(codigo.estaUsado()).isTrue();
    }
}
