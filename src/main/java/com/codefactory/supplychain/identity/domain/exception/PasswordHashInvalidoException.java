package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class PasswordHashInvalidoException extends ReglaDeNegocioException {

    public PasswordHashInvalidoException(String message) {
        super(message);
    }
}
