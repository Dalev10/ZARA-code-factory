package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record AsignarTiendaRequest(
        @NotNull(message = "el tiendaId es obligatorio")
        UUID tiendaId) {
}
