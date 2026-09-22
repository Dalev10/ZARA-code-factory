package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.ScopeEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class ScopeMapper {

    public ScopeEntity toEntity(Scope scope) {
        return ScopeEntity.builder()
                .id(scope.getId())
                .codigo(scope.getCodigo())
                .descripcion(scope.getDescripcion())
                .sensible(scope.isSensible())
                .creadoEn(scope.getCreadoEn())
                .build();
    }

    public Scope toDomain(ScopeEntity entity) {
        return Scope.reconstruir(entity.getId(), entity.getCodigo(), entity.getDescripcion(),
                entity.isSensible(), entity.getCreadoEn());
    }
}
