package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Rol;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface RolRepositoryPort {

    Rol guardar(Rol rol);

    Optional<Rol> buscarPorId(UUID id);

    Optional<Rol> buscarPorNombre(String nombre);

    List<Rol> listarTodos();

    boolean existePorNombre(String nombre);

    void eliminar(UUID id);
}
