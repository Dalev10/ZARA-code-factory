package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.EmailInvalidoException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object que garantiza que ningún Usuario pueda existir con un email vacío,
 * demasiado largo o con formato inválido. Normaliza a minúsculas para que la
 * unicidad de email sea efectivamente insensible a mayúsculas/minúsculas aunque
 * la restricción UNIQUE de la base de datos sea sensible a mayúsculas.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public final class Email {

    private static final int MAX_LENGTH = 255;
    private static final Pattern FORMATO_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String valor;

    private Email(String valor) {
        this.valor = valor;
    }

    public static Email de(String valorCrudo) {
        if (valorCrudo == null || valorCrudo.isBlank()) {
            throw new EmailInvalidoException("El email no puede estar vacío");
        }
        String normalizado = valorCrudo.trim().toLowerCase();
        if (normalizado.length() > MAX_LENGTH) {
            throw new EmailInvalidoException("El email no puede superar " + MAX_LENGTH + " caracteres");
        }
        if (!FORMATO_VALIDO.matcher(normalizado).matches()) {
            throw new EmailInvalidoException("El email no tiene un formato válido");
        }
        return new Email(normalizado);
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return valor.equals(email.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
