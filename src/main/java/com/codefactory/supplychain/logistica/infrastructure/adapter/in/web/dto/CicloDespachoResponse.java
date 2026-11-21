package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import com.codefactory.supplychain.logistica.domain.model.ResultadoDespacho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un ciclo de despacho (HU-46). {@code fillRate} = despachada / solicitada, con cuatro decimales. */
public record CicloDespachoResponse(
        UUID id,
        UUID ordenId,
        int numeroCiclo,
        int cantidadSolicitada,
        int cantidadDespachada,
        int cantidadPendiente,
        ResultadoDespacho resultado,
        BigDecimal fillRate,
        Instant fechaDespacho) {

    public static CicloDespachoResponse desde(CicloDespacho ciclo) {
        return new CicloDespachoResponse(ciclo.getId(), ciclo.getOrdenId(), ciclo.getNumeroCiclo(),
                ciclo.getCantidadSolicitada(), ciclo.getCantidadDespachada(), ciclo.getCantidadPendiente(),
                ciclo.getResultado(), ciclo.getFillRate(), ciclo.getFechaDespacho());
    }
}
