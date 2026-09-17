package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.RolInvalidoException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Rol {

    private static final int NOMBRE_MAX_LENGTH = 100;
    private static final int DESCRIPCION_MAX_LENGTH = 255;

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final Instant creadoEn;

    private Rol(UUID id, String nombre, String descripcion, Instant creadoEn) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.descripcion = validarDescripcion(descripcion);
        this.creadoEn = creadoEn;
    }

    public static Rol crear(String nombre, String descripcion) {
        return new Rol(UUID.randomUUID(), nombre, descripcion, Instant.now());
    }

    public static Rol reconstruir(UUID id, String nombre, String descripcion, Instant creadoEn) {
        return new Rol(id, nombre, descripcion, creadoEn);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new RolInvalidoException("El nombre del rol no puede estar vacío");
        }
        if (nombre.length() > NOMBRE_MAX_LENGTH) {
            throw new RolInvalidoException("El nombre del rol no puede superar " + NOMBRE_MAX_LENGTH + " caracteres");
        }
        return nombre;
    }

    private static String validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX_LENGTH) {
            throw new RolInvalidoException(
                    "La descripción del rol no puede superar " + DESCRIPCION_MAX_LENGTH + " caracteres");
        }
        return descripcion;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol rol)) return false;
        return Objects.equals(id, rol.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Rol[id=" + id + ", nombre=" + nombre + "]";
    }
}
