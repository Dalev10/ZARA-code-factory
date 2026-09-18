package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o modificar una Categoria a través de la
 * capa REST.
 * <p>
 * El tamaño máximo de {@code nombre} (150) es compatible con la columna
 * {@code categoria.nombre} definida en {@code CategoriaEntity}.
 */
public record CategoriaRequest(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 150, message = "El nombre de la categoria no puede superar 150 caracteres")
        String nombre
) {
}
