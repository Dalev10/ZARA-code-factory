package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionConNodoResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.NodoResponse;
import org.springframework.stereotype.Component;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
public class CentroDistribucionWebMapper {

    public CentroDistribucionResponse toResponse(CentroDistribucion centroDistribucion) {
        return new CentroDistribucionResponse(
                centroDistribucion.getId(), centroDistribucion.getNombre(), centroDistribucion.getUbicacion());
    }

    public CentroDistribucionConNodoResponse toResponse(CentroDistribucionConNodo resultado) {
        return new CentroDistribucionConNodoResponse(
                resultado.centroDistribucion().getId(),
                resultado.centroDistribucion().getNombre(),
                resultado.centroDistribucion().getUbicacion(),
                toNodoResponse(resultado.nodo()));
    }

    private NodoResponse toNodoResponse(Nodo nodo) {
        if (nodo == null) {
            return null;
        }
        return new NodoResponse(nodo.getId(), nodo.getTipo(), nodo.getCdId(), nodo.getTiendaId());
    }
}
