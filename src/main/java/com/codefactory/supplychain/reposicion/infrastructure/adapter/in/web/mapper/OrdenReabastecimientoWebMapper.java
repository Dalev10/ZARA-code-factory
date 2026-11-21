package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.reposicion.domain.model.IntentoCd;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.IntentoCdResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.OrdenReabastecimientoResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte de dominio a DTO de respuesta HTTP.
 *
 * Módulo: reposicion.
 */
@Component
public class OrdenReabastecimientoWebMapper {

    public OrdenReabastecimientoResponse toResponse(OrdenReabastecimiento orden) {
        return new OrdenReabastecimientoResponse(
                orden.getId(),
                orden.getRecomendacionId(),
                orden.getTiendaId(),
                orden.getVarianteId(),
                orden.getCdAsignadoId(),
                orden.getMotivoAsignacion(),
                orden.getCantidadTotalSolicitada(),
                orden.getCantidadPendiente(),
                orden.getCantidadDespachadaAcumulada(),
                orden.getEstado(),
                orden.getFechaCreacion(),
                orden.getIntentos().stream().map(OrdenReabastecimientoWebMapper::toResponse).toList(),
                orden.getMotivoCierre(),
                orden.getCerradaPor(),
                orden.getFechaCierre());
    }

    private static IntentoCdResponse toResponse(IntentoCd intento) {
        return new IntentoCdResponse(intento.cdId(), intento.ordenEvaluado(), intento.resultado(),
                intento.motivo(), intento.timestamp());
    }
}
