package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

/**
 * DTO de salida que representa una Categoria en las respuestas REST.
 */
public record CategoriaResponse(
        Long id,
        String nombre
) {
}
