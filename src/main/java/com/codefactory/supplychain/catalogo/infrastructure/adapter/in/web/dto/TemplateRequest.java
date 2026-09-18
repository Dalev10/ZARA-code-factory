package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o modificar un Template a través de la capa
 * REST.
 * <p>
 * Los tamaños máximos de {@code nombre} (200), {@code temporada} (50) y
 * {@code proveedor} (150) son compatibles con las columnas
 * correspondientes definidas en {@code TemplateEntity}. {@code precioBase}
 * es opcional (la columna {@code precio_base} no es {@code NOT NULL}),
 * pero si se envía debe ser mayor o igual a cero.
 */
public record TemplateRequest(

        @NotBlank(message = "El nombre del template es obligatorio")
        @Size(max = 200, message = "El nombre del template no puede superar 200 caracteres")
        String nombre,

        @Size(max = 50, message = "La temporada no puede superar 50 caracteres")
        String temporada,

        @Size(max = 150, message = "El proveedor no puede superar 150 caracteres")
        String proveedor,

        @DecimalMin(value = "0.0", inclusive = true, message = "El precio base no puede ser negativo")
        BigDecimal precioBase,

        @NotNull(message = "El id de la categoria es obligatorio")
        Long categoriaId
) {
}
