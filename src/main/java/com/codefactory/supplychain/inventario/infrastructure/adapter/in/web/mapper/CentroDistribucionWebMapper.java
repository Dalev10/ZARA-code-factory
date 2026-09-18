package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper;

import org.springframework.stereotype.Component;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionResponse;

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