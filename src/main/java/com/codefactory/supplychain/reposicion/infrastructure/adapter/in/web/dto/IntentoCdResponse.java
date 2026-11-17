package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.ResultadoIntentoCd;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Un CD evaluado en el fallback de la orden.
 */
public record IntentoCdResponse(
        UUID cdId,
        @Schema(description = "Posición del CD en la lista de prioridad de la tienda", example = "1")
        int ordenEvaluado,
        @Schema(example = "SIN_STOCK")
        ResultadoIntentoCd resultado,
        @Schema(description = "Motivo del descarte; nulo si se asignó",
                example = "Disponible 4 menor que lo requerido 10")
        String motivo,
        Instant timestamp) {
}
