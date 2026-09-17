package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Scope;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface CrearScopeUseCase {

    Scope crear(CrearScopeComando comando);
}
