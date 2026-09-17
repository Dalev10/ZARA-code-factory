package com.codefactory.supplychain.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa un refresh token emitido a un usuario. Solo guarda el hash del
 * valor real (nunca el token en texto plano — ese vive transitoriamente en la
 * capa de aplicación para poder devolverlo en la cookie, y se descarta).
 *
 * familiaId agrupa todos los tokens descendientes de un mismo login: cada
 * rotación (ver HU-05) emite un token nuevo en la MISMA familia y revoca el
 * anterior. Si un token ya revocado vuelve a presentarse, se interpreta como
 * un posible robo y se revoca la familia completa — de ahí que nunca se borren
 * filas: sin el historial no habría forma de distinguir "nunca existió" de
 * "ya fue usado una vez".
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID familiaId;
    private final String tokenHash;
    private final Instant creadoEn;
    private final Instant expiraEn;
    private final Instant revocadoEn;

    private RefreshToken(UUID id, UUID usuarioId, UUID familiaId, String tokenHash, Instant creadoEn,
                          Instant expiraEn, Instant revocadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.revocadoEn = revocadoEn;
    }

    /**
     * Emite el primer token de una familia nueva (login).
     */
    public static RefreshToken crearNuevaFamilia(UUID usuarioId, String tokenHash, Instant creadoEn,
                                                  Instant expiraEn) {
        return new RefreshToken(UUID.randomUUID(), usuarioId, UUID.randomUUID(), tokenHash, creadoEn, expiraEn, null);
    }

    /**
     * Emite un token sucesor dentro de una familia ya existente (rotación).
     */
    public static RefreshToken crearRotado(UUID usuarioId, UUID familiaId, String tokenHash, Instant creadoEn,
                                            Instant expiraEn) {
        return new RefreshToken(UUID.randomUUID(), usuarioId, familiaId, tokenHash, creadoEn, expiraEn, null);
    }

    public static RefreshToken reconstruir(UUID id, UUID usuarioId, UUID familiaId, String tokenHash,
                                            Instant creadoEn, Instant expiraEn, Instant revocadoEn) {
        return new RefreshToken(id, usuarioId, familiaId, tokenHash, creadoEn, expiraEn, revocadoEn);
    }

    /**
     * Marca este token como revocado (por rotación o por detección de reuse).
     */
    public RefreshToken revocar(Instant ahora) {
        return new RefreshToken(id, usuarioId, familiaId, tokenHash, creadoEn, expiraEn, ahora);
    }

    public boolean estaRevocado() {
        return revocadoEn != null;
    }

    public boolean estaExpirado(Instant ahora) {
        return expiraEn.isBefore(ahora);
    }

    /**
     * Vigente = ni revocado ni expirado. Solo un token vigente puede usarse para refrescar.
     */
    public boolean estaVigente(Instant ahora) {
        return !estaRevocado() && !estaExpirado(ahora);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getFamiliaId() {
        return familiaId;
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

    public Instant getRevocadoEn() {
        return revocadoEn;
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
        return "RefreshToken[id=" + id + ", usuarioId=" + usuarioId + ", familiaId=" + familiaId
                + ", expiraEn=" + expiraEn + ", revocadoEn=" + revocadoEn + "]";
    }
}
