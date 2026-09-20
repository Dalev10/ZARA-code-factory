package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

/**
 * DTO de salida que representa una Variante en las respuestas REST.
 */
public record VarianteResponse(
        Long id,
        String sku,
        String talla,
        String color,
        Long templateId
) {
}
