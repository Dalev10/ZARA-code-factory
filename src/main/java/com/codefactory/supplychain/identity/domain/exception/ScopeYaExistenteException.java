package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class ScopeYaExistenteException extends RecursoDuplicadoException {

    public ScopeYaExistenteException() {
        super("Ya existe un scope con ese código");
    }
}
