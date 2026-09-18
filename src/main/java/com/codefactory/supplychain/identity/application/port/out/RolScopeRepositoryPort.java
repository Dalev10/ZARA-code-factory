package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Scope;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona la relación N-N entre Rol y Scope (tabla rol_scope) — un Rol es, en la
 * práctica, el conjunto de Scopes que otorga. Separado de RolRepositoryPort porque
 * Rol y Scope son agregados independientes; esta es la relación entre ambos, no
 * parte del estado interno de ninguno de los dos.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface RolScopeRepositoryPort {

    void asignar(UUID rolId, UUID scopeId);

    void quitar(UUID rolId, UUID scopeId);

    boolean existeAsignacion(UUID rolId, UUID scopeId);

    List<Scope> listarScopesDeRol(UUID rolId);

    boolean tieneRolesAsignados(UUID scopeId);
}
