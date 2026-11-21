package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de una evaluación manual: las recomendaciones creadas en ella.
 */
public record EvaluacionRecomendacionesResponse(@Schema(description = "Cuántas se crearon en esta evaluación", example = "1")
                                                int recomendacionesCreadas,
                                                List<RecomendacionResponse> recomendaciones) {
}
