package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Recomendación de reposición al CD.
 */
public record RecomendacionResponse(
        UUID id,
        UUID tiendaId,
        UUID varianteId,
        @Schema(description = "CD de mayor prioridad de la tienda, sin validar stock; nulo si la tienda no tiene CDs")
        UUID cdSugeridoId,
        @Schema(description = "Objetivo del nivel menos el pronosticado de la Bodega", example = "12")
        int cantidadSugerida,
        @Schema(example = "BORRADOR")
        EstadoRecomendacion estado,
        @Schema(example = "2026-11-12T15:30:00Z")
        Instant fechaCreacion) {
}
