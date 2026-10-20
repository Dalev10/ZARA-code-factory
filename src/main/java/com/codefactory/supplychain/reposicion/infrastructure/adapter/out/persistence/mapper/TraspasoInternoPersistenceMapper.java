package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.TraspasoInternoEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre {@link TraspasoInterno} (dominio) y {@link TraspasoInternoEntity} (JPA).
 *
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class TraspasoInternoPersistenceMapper {

    public TraspasoInternoEntity toEntity(TraspasoInterno traspaso) {
        return TraspasoInternoEntity.builder()
                .id(traspaso.getId())
                .varianteId(traspaso.getVarianteId())
                .tiendaId(traspaso.getTiendaId())
                .cantidad(traspaso.getCantidad())
                .origen(traspaso.getOrigen())
                .registradoPor(traspaso.getRegistradoPor())
                .timestampTraspaso(traspaso.getTimestampTraspaso())
                .build();
    }

    public TraspasoInterno toDomain(TraspasoInternoEntity entity) {
        return TraspasoInterno.reconstruir(
                entity.getId(),
                entity.getVarianteId(),
                entity.getTiendaId(),
                entity.getCantidad(),
                entity.getOrigen(),
                entity.getRegistradoPor(),
                entity.getTimestampTraspaso()
        );
    }
}
