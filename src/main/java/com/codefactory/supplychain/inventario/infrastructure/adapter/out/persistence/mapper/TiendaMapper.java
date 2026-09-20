package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.TiendaEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
public class TiendaMapper {

    public TiendaEntity toEntity(Tienda tienda) {
        return TiendaEntity.builder()
                .id(tienda.getId())
                .nombre(tienda.getNombre())
                .ubicacion(tienda.getUbicacion())
                .estado(tienda.getEstado())
                .creadoEn(tienda.getCreadoEn())
                .actualizadoEn(tienda.getActualizadoEn())
                .build();
    }

    public Tienda toDomain(TiendaEntity entity) {
        return Tienda.reconstruir(entity.getId(), entity.getNombre(), entity.getUbicacion(),
                entity.getEstado(), entity.getCreadoEn(), entity.getActualizadoEn());
    }
}
