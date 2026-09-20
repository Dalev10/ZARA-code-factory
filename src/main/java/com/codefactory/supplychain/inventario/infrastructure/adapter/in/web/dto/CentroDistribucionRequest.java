package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record CentroDistribucionRequest(

        @NotBlank(message = "El nombre del centro de distribución no puede estar vacío")
        @Size(max = 150, message = "El nombre del centro de distribución no puede tener más de 150 caracteres")
        String nombre,

        @Size(max = 255, message = "La ubicación del centro de distribución no puede tener más de 255 caracteres")
        String ubicacion) {
}
