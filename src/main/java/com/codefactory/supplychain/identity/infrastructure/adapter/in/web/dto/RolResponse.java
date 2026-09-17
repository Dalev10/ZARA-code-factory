package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.identity.domain.model.Rol;

import java.time.Instant;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record RolResponse(UUID id, String nombre, String descripcion, Instant creadoEn) {

    public static RolResponse desde(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.getCreadoEn());
    }
}
