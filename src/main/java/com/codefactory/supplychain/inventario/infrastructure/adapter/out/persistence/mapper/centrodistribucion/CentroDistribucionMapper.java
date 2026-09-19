package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.centrodistribucion;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
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
