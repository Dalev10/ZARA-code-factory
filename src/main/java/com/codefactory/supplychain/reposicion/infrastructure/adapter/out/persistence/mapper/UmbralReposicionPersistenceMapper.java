package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.UmbralReposicionEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre {@link UmbralReposicion} (dominio) y {@link UmbralReposicionEntity} (JPA).
 */
@Component
public class UmbralReposicionPersistenceMapper {

    public UmbralReposicionEntity toEntity(UmbralReposicion umbral) {
        if (umbral == null) {
            return null;
        }
        return new UmbralReposicionEntity(umbral.getId(), umbral.getVarianteId(), umbral.getCategoriaId(),
                umbral.getCantidadMinima(), umbral.getCantidadObjetivo());
    }

    public UmbralReposicion toDomain(UmbralReposicionEntity entity) {
        if (entity == null) {
            return null;
        }
        return UmbralReposicion.reconstruir(entity.getId(), entity.getVarianteId(), entity.getCategoriaId(),
                entity.getCantidadMinima(), entity.getCantidadObjetivo());
    }
}
