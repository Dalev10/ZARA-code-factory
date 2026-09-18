package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.centrodistribucion;

import org.springframework.stereotype.Component;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionResponse;

@Component
public class CentroDistribucionWebMapper {

    public CentroDistribucionResponse toResponse(
            CentroDistribucion centroDistribucion) {

        return new CentroDistribucionResponse(
                centroDistribucion.getId(),
                centroDistribucion.getNombre(),
                centroDistribucion.getUbicacion()
        );
    }
}