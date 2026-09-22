package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import java.util.UUID;

/**
 * DTO de salida que representa una Categoria en las respuestas REST.
 */
public record CategoriaResponse(
        UUID id,
        String nombre
) {
}
