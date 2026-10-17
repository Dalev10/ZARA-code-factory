package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Cuerpo de modificación: solo las cantidades. El alcance (variante o categoría) no cambia.
 */
public record UmbralReposicionCantidadesRequest(
        @NotNull(message = "La cantidad mínima es obligatoria")
        @Positive(message = "La cantidad mínima debe ser mayor que cero")
        Integer cantidadMinima,
        @NotNull(message = "La cantidad objetivo es obligatoria")
        @Positive(message = "La cantidad objetivo debe ser mayor que cero")
        Integer cantidadObjetivo
) {
}
