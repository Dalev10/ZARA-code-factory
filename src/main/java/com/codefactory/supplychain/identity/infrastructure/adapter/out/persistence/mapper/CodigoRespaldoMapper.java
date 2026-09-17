package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.CodigoRespaldoEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class CodigoRespaldoMapper {

    public CodigoRespaldoEntity toEntity(CodigoRespaldo codigo) {
        return CodigoRespaldoEntity.builder()
                .id(codigo.getId())
                .usuarioId(codigo.getUsuarioId())
                .codigoHash(codigo.getCodigoHash())
                .creadoEn(codigo.getCreadoEn())
                .usadoEn(codigo.getUsadoEn())
                .build();
    }

    public CodigoRespaldo toDomain(CodigoRespaldoEntity entity) {
        return CodigoRespaldo.reconstruir(entity.getId(), entity.getUsuarioId(), entity.getCodigoHash(),
                entity.getCreadoEn(), entity.getUsadoEn());
    }
}
