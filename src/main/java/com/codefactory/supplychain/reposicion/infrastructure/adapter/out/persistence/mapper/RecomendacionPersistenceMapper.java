package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.RecomendacionEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre {@link Recomendacion} (dominio) y {@link RecomendacionEntity} (JPA).
 *
 * Módulo: reposicion.
 */
@Component
public class RecomendacionPersistenceMapper {

    public RecomendacionEntity toEntity(Recomendacion recomendacion) {
        return RecomendacionEntity.builder()
                .id(recomendacion.getId())
                .varianteId(recomendacion.getVarianteId())
                .tiendaId(recomendacion.getTiendaId())
                .cdSugeridoId(recomendacion.getCdSugeridoId())
                .cantidadSugerida(recomendacion.getCantidadSugerida())
                .estado(recomendacion.getEstado())
                .fechaCreacion(recomendacion.getFechaCreacion())
                .build();
    }

    public Recomendacion toDomain(RecomendacionEntity entity) {
        return Recomendacion.reconstruir(
                entity.getId(),
                entity.getTiendaId(),
                entity.getVarianteId(),
                entity.getCdSugeridoId(),
                entity.getCantidadSugerida(),
                entity.getEstado(),
                entity.getFechaCreacion());
    }
}
