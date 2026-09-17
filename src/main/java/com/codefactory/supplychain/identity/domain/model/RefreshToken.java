package com.codefactory.supplychain.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa un refresh token emitido a un usuario. Solo guarda el hash del
 * valor real (nunca el token en texto plano — ese vive transitoriamente en la
 * capa de aplicación para poder devolverlo en la cookie, y se descarta).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID usuarioId;
    private final String tokenHash;
    private final Instant creadoEn;
    private final Instant expiraEn;

    private RefreshToken(UUID id, UUID usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public static RefreshToken crear(UUID usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn) {
        return new RefreshToken(UUID.randomUUID(), usuarioId, tokenHash, creadoEn, expiraEn);
    }

    public static RefreshToken reconstruir(UUID id, UUID usuarioId, String tokenHash, Instant creadoEn,
                                            Instant expiraEn) {
        return new RefreshToken(id, usuarioId, tokenHash, creadoEn, expiraEn);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RefreshToken[id=" + id + ", usuarioId=" + usuarioId + ", expiraEn=" + expiraEn + "]";
    }
}
