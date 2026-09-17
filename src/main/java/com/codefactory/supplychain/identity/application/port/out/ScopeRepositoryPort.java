package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Scope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface ScopeRepositoryPort {

    Scope guardar(Scope scope);

    Optional<Scope> buscarPorId(UUID id);

    Optional<Scope> buscarPorCodigo(String codigo);

    List<Scope> listarTodos();

    boolean existePorCodigo(String codigo);

    void eliminar(UUID id);
}
