package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class RolMapper {

    public RolEntity toEntity(Rol rol) {
        return RolEntity.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .creadoEn(rol.getCreadoEn())
                .build();
    }

    public Rol toDomain(RolEntity entity) {
        return Rol.reconstruir(entity.getId(), entity.getNombre(), entity.getDescripcion(), entity.getCreadoEn());
    }
}
