package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto;

import java.util.List;

import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

/**
 * Representación HTTP de una bodega de tienda.
 */
public record BodegaTiendaResponse(
        Long id,
        Long tiendaId,
        TipoNodo tipo,
        String tiendaNombre,
        String tiendaUbicacion,
        List<InventarioResumen> inventario) {

    public static BodegaTiendaResponse from(BodegaTienda bodegaTienda) {
        return new BodegaTiendaResponse(
                bodegaTienda.getId(),
                bodegaTienda.getTiendaId(),
                bodegaTienda.getTipo(),
                null,
                null,
                List.of());
    }

    public static BodegaTiendaResponse from(BodegaTiendaConsulta consulta) {
        return new BodegaTiendaResponse(
                consulta.bodegaTienda().getId(),
                consulta.bodegaTienda().getTiendaId(),
                consulta.bodegaTienda().getTipo(),
                consulta.tienda() == null ? null : consulta.tienda().nombre(),
                consulta.tienda() == null ? null : consulta.tienda().ubicacion(),
                consulta.inventario());
    }
}
