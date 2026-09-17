package com.codefactory.supplychain.identity.domain.exception;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class RolInvalidoException extends RuntimeException {

    public RolInvalidoException(String message) {
        super(message);
    }
}
