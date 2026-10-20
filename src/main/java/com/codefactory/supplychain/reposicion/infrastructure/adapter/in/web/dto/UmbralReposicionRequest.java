package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Cuerpo de creación de un umbral. Debe venir exactamente una de {@code varianteId} y
 * {@code categoriaId}: esa regla la valida el dominio (D-51).
 */
public record UmbralReposicionRequest(
        UUID varianteId,
        UUID categoriaId,
        @NotNull(message = "La cantidad mínima es obligatoria")
        @Positive(message = "La cantidad mínima debe ser mayor que cero")
        Integer cantidadMinima,
        @NotNull(message = "La cantidad objetivo es obligatoria")
        @Positive(message = "La cantidad objetivo debe ser mayor que cero")
        Integer cantidadObjetivo
) {
}
