package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

    public UsuarioNoEncontradoException() {
        super("El usuario solicitado no existe");
    }
}
