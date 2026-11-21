package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de aprobar una recomendación: la aprobación y la orden creada.
 */
public record AprobacionRecomendacionResponse(
        UUID recomendacionId,
        @Schema(example = "APROBADA")
        EstadoRecomendacion estado,
        @Schema(example = "12")
        int cantidadSugerida,
        @Schema(example = "15")
        int cantidadAprobada,
        @Schema(description = "cantidadAprobada − cantidadSugerida. Se calcula, no se guarda", example = "3")
        int desviacion,
        @Schema(description = "Usuario del token que aprobó")
        UUID aprobadoPor,
        @Schema(example = "2026-11-21T14:05:00Z")
        Instant fechaAprobacion,
        @Schema(description = "Orden creada: APROBADA con CD, o GENERADA sin CD con los intentos visibles")
        OrdenReabastecimientoResponse orden) {
}
