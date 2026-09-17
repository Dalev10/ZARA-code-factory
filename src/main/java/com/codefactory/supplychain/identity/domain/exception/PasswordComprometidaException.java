package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class PasswordComprometidaException extends ReglaDeNegocioException {

    public PasswordComprometidaException(String message) {
        super(message);
    }
}
