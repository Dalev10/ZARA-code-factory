package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record TiendaRequest(

        @NotBlank(message = "el nombre de la tienda es obligatorio")
        @Size(max = 150, message = "el nombre de la tienda no puede superar 150 caracteres")
        String nombre,

        @Size(max = 255, message = "la ubicación no puede superar 255 caracteres")
        String ubicacion) {
}
