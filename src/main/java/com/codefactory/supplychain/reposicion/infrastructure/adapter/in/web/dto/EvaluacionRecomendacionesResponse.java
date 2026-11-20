package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * Resultado de una evaluación manual: las recomendaciones creadas en ella.
 */
public record EvaluacionRecomendacionesResponse(int recomendacionesCreadas,
                                                List<RecomendacionResponse> recomendaciones) {
}
