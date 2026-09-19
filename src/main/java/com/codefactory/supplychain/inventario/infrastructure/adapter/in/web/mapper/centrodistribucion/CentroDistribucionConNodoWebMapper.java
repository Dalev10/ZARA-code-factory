package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.centrodistribucion;

import org.springframework.stereotype.Component;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionConNodoResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.NodoResponse;

@Component
public class CentroDistribucionConNodoWebMapper {

    public CentroDistribucionConNodoResponse toResponse(
            CentroDistribucionConNodo resultado) {

        NodoResponse nodoResponse = null;

        if (resultado.getNodoId() != null) {
            nodoResponse = new NodoResponse(
                    resultado.getNodoId(),
                    resultado.getNodoTipo(),
                    resultado.getNodoCdId(),
                    resultado.getNodoTiendaId()
            );
        }

        return new CentroDistribucionConNodoResponse(
                resultado.getId(),
                resultado.getNombre(),
                resultado.getUbicacion(),
                nodoResponse
        );
    }
}