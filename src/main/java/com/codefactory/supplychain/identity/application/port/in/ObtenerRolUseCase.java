package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Rol;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface ObtenerRolUseCase {

    Rol obtener(UUID rolId);
}
