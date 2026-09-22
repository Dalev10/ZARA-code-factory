package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class MfaNoConfiguradoException extends ReglaDeNegocioException {

    public MfaNoConfiguradoException() {
        super("Primero debés iniciar la activación de MFA");
    }
}
