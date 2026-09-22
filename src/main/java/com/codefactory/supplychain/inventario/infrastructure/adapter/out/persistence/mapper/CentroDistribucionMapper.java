package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
public class CentroDistribucionMapper {

    public CentroDistribucionEntity toEntity(CentroDistribucion centroDistribucion) {
        return CentroDistribucionEntity.builder()
                .id(centroDistribucion.getId())
                .nombre(centroDistribucion.getNombre())
                .ubicacion(centroDistribucion.getUbicacion())
                .build();
    }

    public CentroDistribucion toDomain(CentroDistribucionEntity entity) {
        return new CentroDistribucion(entity.getId(), entity.getNombre(), entity.getUbicacion());
    }
}
