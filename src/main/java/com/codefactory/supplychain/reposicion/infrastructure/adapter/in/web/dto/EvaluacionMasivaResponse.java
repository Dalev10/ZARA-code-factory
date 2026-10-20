package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * Resultado de una evaluación masiva de reposición: cuántos traspasos se ejecutaron.
 */
public record EvaluacionMasivaResponse(int traspasoEjecutados, List<TraspasoInternoResponse> traspasos) {
}
