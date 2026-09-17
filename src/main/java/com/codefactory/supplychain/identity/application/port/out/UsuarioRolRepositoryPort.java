package com.codefactory.supplychain.identity.application.port.out;

import java.util.UUID;

/**
 * Lectura de la relación N-N entre Usuario y Rol (tabla usuario_rol). La asignación
 * en sí (HU-10) todavía no existe como caso de uso — esto solo cubre lo que HU-09
 * necesita ya: saber si un Rol sigue en uso antes de borrarlo, y resolver si el
 * usuario autenticado tiene el rol ADMIN para las verificaciones de autorización
 * provisorias hasta que exista el guard genérico de HU-11.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioRolRepositoryPort {

    boolean tieneUsuariosAsignados(UUID rolId);

    boolean usuarioTieneRolNombrado(UUID usuarioId, String nombreRol);
}
