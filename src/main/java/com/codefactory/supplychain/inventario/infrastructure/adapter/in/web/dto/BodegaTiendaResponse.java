package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record BodegaTiendaResponse(
        UUID id,
        UUID tiendaId,
        String tiendaNombre,
        String tiendaUbicacion,
        List<InventarioResumen> inventario) {

    public static BodegaTiendaResponse from(Nodo nodo) {
        return new BodegaTiendaResponse(nodo.getId(), nodo.getTiendaId(), null, null, List.of());
    }

    public static BodegaTiendaResponse from(BodegaTiendaConsulta consulta) {
        Tienda tienda = consulta.tienda();
        return new BodegaTiendaResponse(
                consulta.nodo().getId(),
                consulta.nodo().getTiendaId(),
                tienda == null ? null : tienda.getNombre(),
                tienda == null ? null : tienda.getUbicacion(),
                consulta.inventario());
    }
}
