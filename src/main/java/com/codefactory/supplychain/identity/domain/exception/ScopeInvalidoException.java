package com.codefactory.supplychain.identity.domain.exception;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class ScopeInvalidoException extends RuntimeException {

    public ScopeInvalidoException(String message) {
        super(message);
    }
}
