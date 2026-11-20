package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.RecomendacionResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte de dominio a DTO de respuesta HTTP.
 *
 * Módulo: reposicion.
 */
@Component
public class RecomendacionWebMapper {

    public RecomendacionResponse toResponse(Recomendacion recomendacion) {
        return new RecomendacionResponse(
                recomendacion.getId(),
                recomendacion.getTiendaId(),
                recomendacion.getVarianteId(),
                recomendacion.getCdSugeridoId(),
                recomendacion.getCantidadSugerida(),
                recomendacion.getEstado(),
                recomendacion.getFechaCreacion());
    }
}
