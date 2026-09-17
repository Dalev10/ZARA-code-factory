package com.codefactory.supplychain.identity.application.port.in;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ActivarMfaComando(UUID usuarioId) {
}
