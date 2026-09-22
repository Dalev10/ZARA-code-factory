package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class ScopeEnUsoException extends ReglaDeNegocioException {

    public ScopeEnUsoException() {
        super("El scope está asignado a uno o más roles; quitá esas asignaciones antes de eliminarlo");
    }
}
