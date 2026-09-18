package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import org.springframework.stereotype.Component;

@Component 
public class CentroDistribucionMapper {
    
    public CentroDistribucion toDomain(CentroDistribucionEntity entity) {
        return new CentroDistribucion(
                entity.getId(),
                entity.getNombre(),
                entity.getUbicacion()
        );
    }

    public CentroDistribucionEntity toEntity(
            CentroDistribucion centroDistribucion) {

        CentroDistribucionEntity entity = new CentroDistribucionEntity();

        entity.setId(centroDistribucion.getId());
        entity.setNombre(centroDistribucion.getNombre());
        entity.setUbicacion(centroDistribucion.getUbicacion());

        return entity;
    }
}
