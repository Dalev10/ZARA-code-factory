package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;

import java.time.Instant;
import java.util.UUID;

public record SolicitudNoSatisfechaResponse(UUID id, UUID tiendaId, UUID varianteId, int cantidad,
                                            Integer stockSistema, Instant registradaEn, UUID registradoPor,
                                            String claveIdempotencia, EstadoSolicitud estado,
                                            UUID anuladaPor, Instant anuladaEn) {

    public static SolicitudNoSatisfechaResponse desde(SolicitudNoSatisfecha s) {
        return new SolicitudNoSatisfechaResponse(s.getId(), s.getTiendaId(), s.getVarianteId(), s.getCantidad(),
                s.getStockSistema(), s.getRegistradaEn(), s.getRegistradoPor(), s.getClaveIdempotencia(),
                s.getEstado(), s.getAnuladaPor(), s.getAnuladaEn());
    }
}
