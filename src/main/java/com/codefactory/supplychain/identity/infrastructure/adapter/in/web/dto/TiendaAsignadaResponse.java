package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.identity.application.port.out.TiendaAsignable;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record TiendaAsignadaResponse(UUID id, String nombre, boolean activa) {

    public static TiendaAsignadaResponse desde(TiendaAsignable tienda) {
        return new TiendaAsignadaResponse(tienda.id(), tienda.nombre(), tienda.activa());
    }
}
