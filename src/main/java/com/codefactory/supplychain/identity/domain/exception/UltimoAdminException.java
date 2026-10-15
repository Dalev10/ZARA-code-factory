package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza al quitar el rol ADMIN o al desactivar a un usuario si eso dejaría al sistema
 * sin ningún ADMIN activo (un ADMIN desactivado no puede administrar).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class UltimoAdminException extends ReglaDeNegocioException {

    public UltimoAdminException() {
        super("La operación dejaría al sistema sin ningún usuario ADMIN activo");
    }
}
