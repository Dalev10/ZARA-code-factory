package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class EmailYaRegistradoException extends RecursoDuplicadoException {

    public EmailYaRegistradoException(String message) {
        super(message);
    }
}
