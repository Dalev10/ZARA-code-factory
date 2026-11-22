package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Cuerpo opcional de la aprobación. Sin {@code cantidadAprobada}, se aprueba la sugerida.
 */
public record AprobarRecomendacionRequest(
        @Schema(description = "Cantidad a pedir; si se omite, se aprueba la sugerida", example = "15")
        @Positive(message = "La cantidad aprobada debe ser mayor que cero")
        Integer cantidadAprobada
) {
}
