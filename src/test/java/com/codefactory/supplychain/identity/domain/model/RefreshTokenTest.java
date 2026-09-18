package com.codefactory.supplychain.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void crearNuevaFamiliaAsignaIdYFamiliaDistintosYConservaLosDatos() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(3600);

        RefreshToken token = RefreshToken.crearNuevaFamilia(usuarioId, "hash-de-prueba", ahora, expira);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getFamiliaId()).isNotNull();
        assertThat(token.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(token.getTokenHash()).isEqualTo("hash-de-prueba");
        assertThat(token.getCreadoEn()).isEqualTo(ahora);
        assertThat(token.getExpiraEn()).isEqualTo(expira);
        assertThat(token.getRevocadoEn()).isNull();
        assertThat(token.estaRevocado()).isFalse();
        assertThat(token.estaVigente(ahora)).isTrue();
    }

    @Test
    void dosTokensDeLoginsDistintosTienenFamiliasDistintas() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();

        RefreshToken token1 = RefreshToken.crearNuevaFamilia(usuarioId, "hash-1", ahora, ahora.plusSeconds(3600));
        RefreshToken token2 = RefreshToken.crearNuevaFamilia(usuarioId, "hash-2", ahora, ahora.plusSeconds(3600));

        assertThat(token1.getFamiliaId()).isNotEqualTo(token2.getFamiliaId());
    }

    @Test
    void crearRotadoConservaLaFamiliaDelTokenOriginal() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        RefreshToken original = RefreshToken.crearNuevaFamilia(usuarioId, "hash-original", ahora,
                ahora.plusSeconds(3600));

        RefreshToken rotado = RefreshToken.crearRotado(usuarioId, original.getFamiliaId(), "hash-rotado", ahora,
                ahora.plusSeconds(3600));

        assertThat(rotado.getFamiliaId()).isEqualTo(original.getFamiliaId());
        assertThat(rotado.getId()).isNotEqualTo(original.getId());
    }

    @Test
    void revocarFijaRevocadoEnYDejaDeEstarVigente() {
        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.crearNuevaFamilia(UUID.randomUUID(), "hash", ahora,
                ahora.plusSeconds(3600));

        RefreshToken revocado = token.revocar(ahora);

        assertThat(revocado.getRevocadoEn()).isEqualTo(ahora);
        assertThat(revocado.estaRevocado()).isTrue();
        assertThat(revocado.estaVigente(ahora)).isFalse();
    }

    @Test
    void unTokenExpiradoNoEstaVigenteAunqueNoEsteRevocado() {
        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.crearNuevaFamilia(UUID.randomUUID(), "hash", ahora.minusSeconds(7200),
                ahora.minusSeconds(3600));

        assertThat(token.estaExpirado(ahora)).isTrue();
        assertThat(token.estaRevocado()).isFalse();
        assertThat(token.estaVigente(ahora)).isFalse();
    }

    @Test
    void dosTokensSonIgualesSoloSiTienenElMismoId() {
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        RefreshToken token1 = RefreshToken.reconstruir(id, UUID.randomUUID(), UUID.randomUUID(), "hash-1",
                ahora, ahora, null);
        RefreshToken token2 = RefreshToken.reconstruir(id, UUID.randomUUID(), UUID.randomUUID(), "hash-2",
                ahora, ahora, ahora);

        assertThat(token1).isEqualTo(token2);
    }

    @Test
    void reconstruirPreservaElEstadoPersistido() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID familiaId = UUID.randomUUID();
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant expira = Instant.parse("2026-01-08T00:00:00Z");
        Instant revocado = Instant.parse("2026-01-02T00:00:00Z");

        RefreshToken token = RefreshToken.reconstruir(id, usuarioId, familiaId, "hash-persistido", creado, expira,
                revocado);

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(token.getFamiliaId()).isEqualTo(familiaId);
        assertThat(token.getTokenHash()).isEqualTo("hash-persistido");
        assertThat(token.getCreadoEn()).isEqualTo(creado);
        assertThat(token.getExpiraEn()).isEqualTo(expira);
        assertThat(token.getRevocadoEn()).isEqualTo(revocado);
        assertThat(token.estaRevocado()).isTrue();
    }
}
