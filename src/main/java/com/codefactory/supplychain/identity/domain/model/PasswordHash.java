package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.PasswordHashInvalidoException;

import java.util.Objects;

/**
 * Envuelve el hash de contraseña ya calculado. El dominio deliberadamente no conoce
 * ni valida el algoritmo de hasheo usado (BCrypt, Argon2, etc.) — eso es una decisión
 * de infraestructura/aplicación — solo garantiza que nunca se almacene un valor vacío
 * o fuera de rango, y evita que el hash se filtre por accidente en logs (ver toString).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class PasswordHash {

    private static final int MAX_LENGTH = 255;

    private final String valor;

    private PasswordHash(String valor) {
        this.valor = valor;
    }

    public static PasswordHash de(String valorHasheado) {
        if (valorHasheado == null || valorHasheado.isBlank()) {
            throw new PasswordHashInvalidoException("El hash de contraseña no puede estar vacío");
        }
        if (valorHasheado.length() > MAX_LENGTH) {
            throw new PasswordHashInvalidoException(
                    "El hash de contraseña no puede superar " + MAX_LENGTH + " caracteres");
        }
        return new PasswordHash(valorHasheado);
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordHash that)) return false;
        return valor.equals(that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return "PasswordHash[PROTEGIDO]";
    }
}
