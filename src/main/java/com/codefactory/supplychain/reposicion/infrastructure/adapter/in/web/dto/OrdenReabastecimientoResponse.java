package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orden de reabastecimiento con sus intentos de CD.
 */
public record OrdenReabastecimientoResponse(
        UUID id,
        UUID recomendacionId,
        UUID tiendaId,
        UUID varianteId,
        @Schema(description = "CD asignado; nulo si la orden quedó GENERADA sin CD")
        UUID cdAsignadoId,
        String motivoAsignacion,
        @Schema(example = "10")
        int cantidadTotalSolicitada,
        @Schema(example = "10")
        int cantidadPendiente,
        @Schema(example = "0")
        int cantidadDespachadaAcumulada,
        @Schema(example = "APROBADA")
        EstadoOrden estado,
        Instant fechaCreacion,
        List<IntentoCdResponse> intentos) {
}
