package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.ResultadoRecepcion;
import com.codefactory.supplychain.logistica.domain.model.Recepcion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Recepción registrada y situación de la orden tras registrarla (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Schema(description = "Recepción de una parada y estado de la orden de reabastecimiento")
public record RecepcionResponse(
        UUID id,
        UUID viajeParadaId,
        UUID cicloDespachoId,
        @Schema(description = "Unidades que salieron del CD en este ciclo", example = "10")
        int cantidadDespachada,
        @Schema(example = "8")
        int cantidadRecibida,
        @Schema(description = "Despachada − recibida: pérdida en tránsito, sin movimiento de kardex", example = "2")
        int discrepancia,
        UUID registradoPor,
        Instant fechaRecepcion,
        @Schema(description = "Estado de la orden: CERRADA si no queda pendiente o REABIERTA si queda",
                example = "REABIERTA")
        String estadoOrden,
        @Schema(description = "Solicitado − recibido acumulado de la orden", example = "2")
        int cantidadPendienteOrden) {

    public static RecepcionResponse from(ResultadoRecepcion resultado) {
        Recepcion r = resultado.recepcion();
        return new RecepcionResponse(r.getId(), r.getViajeParadaId(), r.getCicloDespachoId(), r.cantidadDespachada(),
                r.getCantidadRecibida(), r.getDiscrepancia(), r.getRegistradoPor(), r.getFechaRecepcion(),
                resultado.estadoOrden(), resultado.cantidadPendiente());
    }
}
