package com.codefactory.supplychain.identity.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Relación N:M entre Usuario y Tienda (tabla usuario_tienda, HU-32).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioTiendaRepositoryPort {

    void asignar(UUID usuarioId, UUID tiendaId);

    void quitar(UUID usuarioId, UUID tiendaId);

    boolean existeAsignacion(UUID usuarioId, UUID tiendaId);

    List<UUID> listarTiendasDeUsuario(UUID usuarioId);
}
