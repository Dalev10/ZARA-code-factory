package com.codefactory.supplychain.identity.domain.exception;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class PasswordHashInvalidoException extends RuntimeException {

    public PasswordHashInvalidoException(String message) {
        super(message);
    }
}
