package com.codefactory.supplychain.identity.application.port.in;

import java.util.UUID;

/**
 * actorId es quien ejecuta la operación (sale del token, nunca del body): se usa para
 * impedir que un usuario se desactive a sí mismo.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record DesactivarUsuarioComando(UUID usuarioId, UUID actorId) {
}
