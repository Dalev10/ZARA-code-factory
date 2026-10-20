package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.time.Instant;
import java.util.UUID;

/**
 * Tienda con los ids de sus nodos ALMACEN y BODEGA_TIENDA, que piden la carga inicial, los
 * ajustes y las consultas de inventario ({@code /nodos/{nodoId}/inventario}). Mismo criterio
 * que {@link CentroDistribucionConNodoResponse}.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record TiendaConNodosResponse(
        UUID id,
        String nombre,
        String ubicacion,
        String estado,
        Instant creadoEn,
        Instant actualizadoEn,
        UUID almacenId,
        UUID bodegaId) {

    public static TiendaConNodosResponse desde(Tienda tienda, UUID almacenId, UUID bodegaId) {
        return new TiendaConNodosResponse(tienda.getId(), tienda.getNombre(), tienda.getUbicacion(),
                tienda.getEstado().name(), tienda.getCreadoEn(), tienda.getActualizadoEn(), almacenId, bodegaId);
    }
}
