package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import java.util.UUID;

/**
 * DTO de salida que representa una Variante en las respuestas REST.
 */
public record VarianteResponse(
        UUID id,
        String sku,
        String talla,
        String color,
        UUID templateId
) {
}
