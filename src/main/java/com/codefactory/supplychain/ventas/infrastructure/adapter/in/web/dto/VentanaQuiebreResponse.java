package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;

import java.time.Instant;
import java.util.UUID;

public record VentanaQuiebreResponse(UUID id, UUID tiendaId, UUID varianteId, Instant fechaInicio,
                                     Instant fechaFin, MotivoCierreVentana motivoCierre, String estado) {

    public static VentanaQuiebreResponse desde(VentanaQuiebre v) {
        return new VentanaQuiebreResponse(v.getId(), v.getTiendaId(), v.getVarianteId(), v.getFechaInicio(),
                v.getFechaFin(), v.getMotivoCierre(), v.estaAbierta() ? "ABIERTA" : "CERRADA");
    }
}
