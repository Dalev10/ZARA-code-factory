package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record RegistrarBodegaTiendaRequest(

        @NotNull(message = "el id de la tienda es obligatorio")
        UUID tiendaId) {
}
