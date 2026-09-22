package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class RolNoEncontradoException extends RecursoNoEncontradoException {

    public RolNoEncontradoException() {
        super("El rol solicitado no existe");
    }
}
