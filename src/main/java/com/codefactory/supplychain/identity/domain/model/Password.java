package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.PasswordDebilException;

import java.util.Objects;

/**
 * Value Object que representa una contraseña candidata en texto plano, antes de ser
 * hasheada. Solo vive transitoriamente durante el registro/cambio de contraseña —
 * nunca se persiste ni se expone (ver toString). Impone la política de longitud
 * mínima (12 caracteres, sin reglas de complejidad — ver decisión registrada:
 * la longitud pesa más que forzar mayúscula/número/símbolo, que en la práctica
 * empuja a patrones predecibles). El chequeo contra contraseñas filtradas
 * (HaveIBeenPwned) es una verificación externa y vive en la capa de aplicación,
 * no en este Value Object.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Password {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private final String valor;

    private Password(String valor) {
        this.valor = valor;
    }

    public static Password de(String valorCrudo) {
        if (valorCrudo == null || valorCrudo.isBlank()) {
            throw new PasswordDebilException("La contraseña no puede estar vacía");
        }
        if (valorCrudo.length() < MIN_LENGTH) {
            throw new PasswordDebilException("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (valorCrudo.length() > MAX_LENGTH) {
            throw new PasswordDebilException("La contraseña no puede superar " + MAX_LENGTH + " caracteres");
        }
        return new Password(valorCrudo);
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Password password)) return false;
        return valor.equals(password.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return "Password[PROTEGIDA]";
    }
}
