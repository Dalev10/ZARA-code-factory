package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Rol;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona la relación N-N entre Usuario y Rol (tabla usuario_rol). La lectura
 * (tieneUsuariosAsignados) nació en HU-09 para el guard de borrado de Rol;
 * HU-10 agrega la asignación real.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioRolRepositoryPort {

    boolean tieneUsuariosAsignados(UUID rolId);

    void asignar(UUID usuarioId, UUID rolId);

    void quitar(UUID usuarioId, UUID rolId);

    boolean existeAsignacion(UUID usuarioId, UUID rolId);

    List<Rol> listarRolesDeUsuario(UUID usuarioId);

    long contarUsuariosConRol(UUID rolId);
}
