package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import java.util.UUID;
import java.math.BigDecimal;

/**
 * DTO de salida que representa un Template en las respuestas REST.
 */
public record TemplateResponse(
        UUID id,
        String nombre,
        String temporada,
        String proveedor,
        BigDecimal precioBase,
        UUID categoriaId
) {
}
