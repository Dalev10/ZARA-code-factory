package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto;

import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

/**
 * Representación HTTP de una bodega de tienda.
 */
public record BodegaTiendaResponse(Long id, Long tiendaId, TipoNodo tipo) {

    public static BodegaTiendaResponse from(BodegaTienda bodegaTienda) {
        return new BodegaTiendaResponse(
                bodegaTienda.getId(),
                bodegaTienda.getTiendaId(),
                bodegaTienda.getTipo());
    }
}
