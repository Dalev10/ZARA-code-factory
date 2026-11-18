package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class RolEnUsoException extends ReglaDeNegocioException {

    public RolEnUsoException() {
        super("El rol tiene usuarios asignados; se deben quitar esas asignaciones antes de eliminarlo");
    }
}
