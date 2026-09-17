package com.codefactory.supplychain.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Código de un solo uso para iniciar sesión si el usuario pierde acceso a su
 * aplicación TOTP. Solo guarda el hash del valor real — el valor en texto plano
 * se muestra una única vez al generarlo y nunca se persiste.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class CodigoRespaldo {

    private final UUID id;
    private final UUID usuarioId;
    private final String codigoHash;
    private final Instant creadoEn;
    private final Instant usadoEn;

    private CodigoRespaldo(UUID id, UUID usuarioId, String codigoHash, Instant creadoEn, Instant usadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.codigoHash = codigoHash;
        this.creadoEn = creadoEn;
        this.usadoEn = usadoEn;
    }

    public static CodigoRespaldo crear(UUID usuarioId, String codigoHash, Instant creadoEn) {
        return new CodigoRespaldo(UUID.randomUUID(), usuarioId, codigoHash, creadoEn, null);
    }

    public static CodigoRespaldo reconstruir(UUID id, UUID usuarioId, String codigoHash, Instant creadoEn,
                                              Instant usadoEn) {
        return new CodigoRespaldo(id, usuarioId, codigoHash, creadoEn, usadoEn);
    }

    public CodigoRespaldo marcarUsado(Instant ahora) {
        return new CodigoRespaldo(id, usuarioId, codigoHash, creadoEn, ahora);
    }

    public boolean estaUsado() {
        return usadoEn != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getUsadoEn() {
        return usadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodigoRespaldo that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "CodigoRespaldo[id=" + id + ", usuarioId=" + usuarioId + ", usadoEn=" + usadoEn + "]";
    }
}
