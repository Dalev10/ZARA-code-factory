package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.NombreCompletoInvalidoException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raíz del módulo identity. Representa una cuenta de usuario del sistema.
 * Deliberadamente NO carga sus roles ni scopes como parte del agregado — esa relación
 * se resuelve mediante un puerto de consulta aparte (ver HU de autorización), para no
 * acoplar cada carga de Usuario al grafo completo de autorización.
 *
 * Los atributos de MFA y bloqueo por intentos fallidos existen en el modelo desde ya
 * porque son parte del esquema (V2), pero el comportamiento que los muta (bloquear,
 * habilitar MFA, etc.) se agrega incrementalmente en las HU que lo necesitan.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Usuario {

    private static final int NOMBRE_COMPLETO_MAX_LENGTH = 150;

    private final UUID id;
    private final Email email;
    private final String nombreCompleto;
    private final PasswordHash passwordHash;
    private final EstadoUsuario estado;
    private final int intentosFallidos;
    private final Instant bloqueadoHasta;
    private final boolean mfaHabilitado;
    private final String mfaSecretEncrypted;
    private final String proveedorExterno;
    private final Instant creadoEn;
    private final Instant actualizadoEn;

    private Usuario(UUID id, Email email, String nombreCompleto, PasswordHash passwordHash,
                     EstadoUsuario estado, int intentosFallidos, Instant bloqueadoHasta,
                     boolean mfaHabilitado, String mfaSecretEncrypted, String proveedorExterno,
                     Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.email = email;
        this.nombreCompleto = validarNombreCompleto(nombreCompleto);
        this.passwordHash = passwordHash;
        this.estado = estado;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.mfaHabilitado = mfaHabilitado;
        this.mfaSecretEncrypted = mfaSecretEncrypted;
        this.proveedorExterno = proveedorExterno;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /**
     * Da de alta un usuario nuevo con los valores por defecto de toda cuenta recién creada:
     * ACTIVO, sin intentos fallidos, sin MFA habilitado.
     */
    public static Usuario crear(Email email, String nombreCompleto, PasswordHash passwordHash) {
        Instant ahora = Instant.now();
        return new Usuario(UUID.randomUUID(), email, nombreCompleto, passwordHash,
                EstadoUsuario.ACTIVO, 0, null, false, null, null, ahora, ahora);
    }

    /**
     * Reconstruye un usuario ya existente a partir de su estado persistido.
     * Uso exclusivo de los mappers de infraestructura — nunca desde application/service.
     */
    public static Usuario reconstruir(UUID id, Email email, String nombreCompleto, PasswordHash passwordHash,
                                       EstadoUsuario estado, int intentosFallidos, Instant bloqueadoHasta,
                                       boolean mfaHabilitado, String mfaSecretEncrypted, String proveedorExterno,
                                       Instant creadoEn, Instant actualizadoEn) {
        return new Usuario(id, email, nombreCompleto, passwordHash, estado, intentosFallidos, bloqueadoHasta,
                mfaHabilitado, mfaSecretEncrypted, proveedorExterno, creadoEn, actualizadoEn);
    }

    private static String validarNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new NombreCompletoInvalidoException("El nombre completo no puede estar vacío");
        }
        if (nombreCompleto.length() > NOMBRE_COMPLETO_MAX_LENGTH) {
            throw new NombreCompletoInvalidoException(
                    "El nombre completo no puede superar " + NOMBRE_COMPLETO_MAX_LENGTH + " caracteres");
        }
        return nombreCompleto;
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public boolean isMfaHabilitado() {
        return mfaHabilitado;
    }

    public String getMfaSecretEncrypted() {
        return mfaSecretEncrypted;
    }

    public String getProveedorExterno() {
        return proveedorExterno;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario usuario)) return false;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", email=" + email + ", estado=" + estado + "]";
    }
}
