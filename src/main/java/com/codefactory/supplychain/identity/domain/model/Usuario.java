package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.MfaNoConfiguradoException;
import com.codefactory.supplychain.identity.domain.exception.MfaYaActivoException;
import com.codefactory.supplychain.identity.domain.exception.NombreCompletoInvalidoException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raíz del módulo identity. Representa una cuenta de usuario del sistema.
 * Deliberadamente NO carga sus roles ni scopes como parte del agregado — esa relación
 * se resuelve mediante un puerto de consulta aparte (ver HU de autorización), para no
 * acoplar cada carga de Usuario al grafo completo de autorización.
 *
 * Los atributos de MFA existen en el modelo desde HU-01 porque son parte del esquema
 * (V2), pero el comportamiento que los muta se agrega incrementalmente en las HU que
 * lo necesitan (ver {@link #registrarIntentoFallido} y {@link #registrarLoginExitoso}
 * para el bloqueo progresivo).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Usuario {

    private static final int NOMBRE_COMPLETO_MAX_LENGTH = 150;

    // Curva de bloqueo progresivo: a partir de N intentos fallidos CONSECUTIVOS
    // (el contador solo se resetea con un login exitoso, nunca con el simple paso
    // del tiempo), se bloquea la cuenta por la duración asociada. El bloqueo nunca
    // es permanente — el techo es 1 hora, no sigue escalando después del 4to nivel.
    private static final int UMBRAL_NIVEL_1 = 3;
    private static final int UMBRAL_NIVEL_2 = 5;
    private static final int UMBRAL_NIVEL_3 = 7;
    private static final int UMBRAL_NIVEL_4 = 10;

    private static final Duration BLOQUEO_NIVEL_1 = Duration.ofMinutes(1);
    private static final Duration BLOQUEO_NIVEL_2 = Duration.ofMinutes(5);
    private static final Duration BLOQUEO_NIVEL_3 = Duration.ofMinutes(15);
    private static final Duration BLOQUEO_NIVEL_4 = Duration.ofHours(1);

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

    /**
     * Registra un intento de login fallido: incrementa el contador y, si se cruza
     * alguno de los umbrales de la curva de bloqueo progresivo, fija bloqueadoHasta.
     * El contador NUNCA se resetea por el simple paso del tiempo — solo con un login
     * exitoso ({@link #registrarLoginExitoso}) — así que si se sigue fallando después
     * de que un bloqueo expira, el siguiente fallo salta directo al próximo escalón.
     */
    public Usuario registrarIntentoFallido(Instant ahora) {
        int nuevosIntentos = this.intentosFallidos + 1;
        Instant nuevoBloqueoHasta = calcularBloqueoHasta(nuevosIntentos, ahora);
        return new Usuario(id, email, nombreCompleto, passwordHash, estado, nuevosIntentos, nuevoBloqueoHasta,
                mfaHabilitado, mfaSecretEncrypted, proveedorExterno, creadoEn, ahora);
    }

    /**
     * Registra un login exitoso: es el único evento que resetea el contador de
     * intentos fallidos y limpia cualquier bloqueo vigente.
     */
    public Usuario registrarLoginExitoso(Instant ahora) {
        return new Usuario(id, email, nombreCompleto, passwordHash, estado, 0, null,
                mfaHabilitado, mfaSecretEncrypted, proveedorExterno, creadoEn, ahora);
    }

    /**
     * true si hay un bloqueo por intentos fallidos vigente en este momento. No debe
     * confundirse con {@code estado == BLOQUEADO} (bloqueo administrativo/permanente,
     * sin mecanismo de auto-expiración) — este es siempre temporal.
     */
    public boolean estaBloqueadoTemporalmente(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /**
     * Primer paso de la activación de MFA: guarda el secreto TOTP (ya cifrado por la
     * capa de aplicación — el dominio nunca ve el secreto en texto plano) sin habilitar
     * MFA todavía. Recién queda habilitado tras {@link #confirmarActivacionMfa}, una vez
     * que el usuario demuestra que configuró su app TOTP correctamente.
     */
    public Usuario iniciarActivacionMfa(String secretoCifrado, Instant ahora) {
        if (mfaHabilitado) {
            throw new MfaYaActivoException();
        }
        return new Usuario(id, email, nombreCompleto, passwordHash, estado, intentosFallidos, bloqueadoHasta,
                false, secretoCifrado, proveedorExterno, creadoEn, ahora);
    }

    /**
     * Segundo paso: habilita MFA. Requiere que {@link #iniciarActivacionMfa} ya haya
     * dejado un secreto pendiente de confirmar.
     */
    public Usuario confirmarActivacionMfa(Instant ahora) {
        if (mfaSecretEncrypted == null) {
            throw new MfaNoConfiguradoException();
        }
        return new Usuario(id, email, nombreCompleto, passwordHash, estado, intentosFallidos, bloqueadoHasta,
                true, mfaSecretEncrypted, proveedorExterno, creadoEn, ahora);
    }

    private static Instant calcularBloqueoHasta(int intentosFallidosConsecutivos, Instant ahora) {
        if (intentosFallidosConsecutivos >= UMBRAL_NIVEL_4) {
            return ahora.plus(BLOQUEO_NIVEL_4);
        }
        if (intentosFallidosConsecutivos >= UMBRAL_NIVEL_3) {
            return ahora.plus(BLOQUEO_NIVEL_3);
        }
        if (intentosFallidosConsecutivos >= UMBRAL_NIVEL_2) {
            return ahora.plus(BLOQUEO_NIVEL_2);
        }
        if (intentosFallidosConsecutivos >= UMBRAL_NIVEL_1) {
            return ahora.plus(BLOQUEO_NIVEL_1);
        }
        return null;
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
