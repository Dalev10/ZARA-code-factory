package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.time.Instant;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record TiendaResponse(
        UUID id,
        String nombre,
        String ubicacion,
        String estado,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static TiendaResponse desde(Tienda tienda) {
        return new TiendaResponse(tienda.getId(), tienda.getNombre(), tienda.getUbicacion(),
                tienda.getEstado().name(), tienda.getCreadoEn(), tienda.getActualizadoEn());
    }
}
