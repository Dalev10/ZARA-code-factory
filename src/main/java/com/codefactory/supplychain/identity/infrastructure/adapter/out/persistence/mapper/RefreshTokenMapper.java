package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class RefreshTokenMapper {

    public RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        return RefreshTokenEntity.builder()
                .id(refreshToken.getId())
                .usuarioId(refreshToken.getUsuarioId())
                .familiaId(refreshToken.getFamiliaId())
                .tokenHash(refreshToken.getTokenHash())
                .creadoEn(refreshToken.getCreadoEn())
                .expiraEn(refreshToken.getExpiraEn())
                .revocadoEn(refreshToken.getRevocadoEn())
                .build();
    }

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.reconstruir(entity.getId(), entity.getUsuarioId(), entity.getFamiliaId(),
                entity.getTokenHash(), entity.getCreadoEn(), entity.getExpiraEn(), entity.getRevocadoEn());
    }
}
