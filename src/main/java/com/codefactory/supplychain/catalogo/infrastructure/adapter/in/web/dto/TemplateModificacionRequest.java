package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de entrada para modificar un Template. No lleva la categoría: un Template no cambia de
 * categoría (ver {@code Template#actualizarInformacion}), así que pedirla sería engañoso. Un
 * cliente que la siga enviando no falla: Jackson ignora los campos desconocidos.
 * <p>
 * Mismos límites que {@link TemplateRequest}.
 */
public record TemplateModificacionRequest(

        @NotBlank(message = "El nombre del template es obligatorio")
        @Size(max = 200, message = "El nombre del template no puede superar 200 caracteres")
        String nombre,

        @Size(max = 50, message = "La temporada no puede superar 50 caracteres")
        String temporada,

        @Size(max = 150, message = "El proveedor no puede superar 150 caracteres")
        String proveedor,

        @DecimalMin(value = "0.0", inclusive = true, message = "El precio base no puede ser negativo")
        BigDecimal precioBase
) {
}
