package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class UltimoAdminException extends ReglaDeNegocioException {

    public UltimoAdminException() {
        super("No podés quitar el rol ADMIN al único usuario que lo tiene");
    }
}
