package com.codefactory.supplychain.identity.domain.exception;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class NombreCompletoInvalidoException extends RuntimeException {

    public NombreCompletoInvalidoException(String message) {
        super(message);
    }
}
