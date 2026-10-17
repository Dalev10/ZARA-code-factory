package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce el agregado Inventario a su representación persistente.
 */
@Component
public class InventarioMapper {

    public InventarioJpaEntity toEntity(Inventario inventario) {
        return InventarioJpaEntity.builder()
                .id(inventario.getId())
                .nodoId(inventario.getNodoId())
                .varianteId(inventario.getVarianteId())
                .aLaMano(inventario.getALaMano())
                .disponibleParaUso(inventario.getDisponible())
                .entrante(inventario.getEntrante())
                .saliente(inventario.getSaliente())
                .pronosticado(inventario.getPronosticado())
                .version(inventario.getVersion())
                .fechaActualizacion(java.time.Instant.now())
                .build();
    }

    public Inventario toDomain(InventarioJpaEntity entity) {
        return Inventario.reconstruir(
                entity.getId(),
                entity.getNodoId(),
                entity.getVarianteId(),
                entity.getALaMano(),
                entity.getEntrante(),
                entity.getSaliente(),
                entity.getVersion());
    }
}
