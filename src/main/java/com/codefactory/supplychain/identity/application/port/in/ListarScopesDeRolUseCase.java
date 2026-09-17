package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Scope;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface ListarScopesDeRolUseCase {

    List<Scope> listar(UUID rolId);
}
