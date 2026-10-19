package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import java.util.UUID;

/**
 * Representación de un umbral. Una de {@code varianteId} y {@code categoriaId} es {@code null}.
 */
public record UmbralReposicionResponse(
        UUID id,
        UUID varianteId,
        UUID categoriaId,
        int cantidadMinima,
        int cantidadObjetivo
) {
}
