package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o modificar una Variante a través de la
 * capa REST.
 * <p>
 * El tamaño máximo de {@code sku} (60) es compatible con la columna
 * {@code variante.sku} definida en {@code VarianteEntity}; los de
 * {@code talla} (20) y {@code color} (50) también. {@code talla} y
 * {@code color} son opcionales: la columna correspondiente permite
 * {@code NULL} y no hay ningún requisito que las haga obligatorias.
 */
public record VarianteRequest(

        @NotBlank(message = "El sku es obligatorio")
        @Size(max = 60, message = "El sku no puede superar 60 caracteres")
        String sku,

        @Size(max = 20, message = "La talla no puede superar 20 caracteres")
        String talla,

        @Size(max = 50, message = "El color no puede superar 50 caracteres")
        String color,

        @NotNull(message = "El id del template es obligatorio")
        Long templateId
) {
}
