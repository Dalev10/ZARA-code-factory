package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos necesarios para registrar una bodega de tienda.
 */
public record RegistrarBodegaTiendaRequest(
        @NotNull(message = "El id de la tienda es obligatorio")
        @Positive(message = "El id de la tienda debe ser mayor a cero")
        Long tiendaId) {
}
