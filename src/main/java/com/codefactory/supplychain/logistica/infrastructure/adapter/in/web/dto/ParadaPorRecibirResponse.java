package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Parada entregada que espera su recepción (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Schema(description = "Parada ENTREGADO sin recepción registrada")
public record ParadaPorRecibirResponse(
        UUID paradaId,
        UUID viajeId,
        UUID tiendaId,
        UUID cicloDespachoId,
        UUID ordenId,
        @Schema(description = "Unidades que salieron del CD para esta parada", example = "10")
        int cantidadDespachada) {

    public static ParadaPorRecibirResponse from(ParadaRecepcion parada) {
        return new ParadaPorRecibirResponse(parada.id(), parada.viajeId(), parada.tiendaId(),
                parada.cicloDespachoId(), parada.ordenId(), parada.cantidadDespachada());
    }
}
