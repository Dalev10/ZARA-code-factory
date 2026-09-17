package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.identity.domain.model.Scope;

import java.time.Instant;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ScopeResponse(UUID id, String codigo, String descripcion, boolean sensible, Instant creadoEn) {

    public static ScopeResponse desde(Scope scope) {
        return new ScopeResponse(scope.getId(), scope.getCodigo(), scope.getDescripcion(), scope.isSensible(),
                scope.getCreadoEn());
    }
}
