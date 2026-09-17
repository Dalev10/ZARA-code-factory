package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.ScopeInvalidoException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Scope {

    private static final int CODIGO_MAX_LENGTH = 100;
    private static final int DESCRIPCION_MAX_LENGTH = 255;

    private final UUID id;
    private final String codigo;
    private final String descripcion;
    private final boolean sensible;
    private final Instant creadoEn;

    private Scope(UUID id, String codigo, String descripcion, boolean sensible, Instant creadoEn) {
        this.id = id;
        this.codigo = validarCodigo(codigo);
        this.descripcion = validarDescripcion(descripcion);
        this.sensible = sensible;
        this.creadoEn = creadoEn;
    }

    public static Scope crear(String codigo, String descripcion, boolean sensible) {
        return new Scope(UUID.randomUUID(), codigo, descripcion, sensible, Instant.now());
    }

    public static Scope reconstruir(UUID id, String codigo, String descripcion, boolean sensible, Instant creadoEn) {
        return new Scope(id, codigo, descripcion, sensible, creadoEn);
    }

    public Scope actualizar(String nuevoCodigo, String nuevaDescripcion, boolean nuevoSensible) {
        return new Scope(id, nuevoCodigo, nuevaDescripcion, nuevoSensible, creadoEn);
    }

    private static String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ScopeInvalidoException("El código del scope no puede estar vacío");
        }
        if (codigo.length() > CODIGO_MAX_LENGTH) {
            throw new ScopeInvalidoException(
                    "El código del scope no puede superar " + CODIGO_MAX_LENGTH + " caracteres");
        }
        return codigo;
    }

    private static String validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX_LENGTH) {
            throw new ScopeInvalidoException(
                    "La descripción del scope no puede superar " + DESCRIPCION_MAX_LENGTH + " caracteres");
        }
        return descripcion;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isSensible() {
        return sensible;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scope scope)) return false;
        return Objects.equals(id, scope.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Scope[id=" + id + ", codigo=" + codigo + ", sensible=" + sensible + "]";
    }
}
