package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/**
 * La tienda que se intenta asignar a un usuario no existe (HU-32).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class TiendaNoEncontradaException extends RecursoNoEncontradoException {

    public TiendaNoEncontradaException() {
        super("La tienda solicitada no existe");
    }
}
