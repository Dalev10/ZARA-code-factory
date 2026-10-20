package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoInternoResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte de dominio a DTO de respuesta HTTP.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class TraspasoInternoWebMapper {

    public TraspasoInternoResponse toResponse(TraspasoInterno traspaso) {
        return new TraspasoInternoResponse(
                traspaso.getId(),
                traspaso.getVarianteId(),
                traspaso.getTiendaId(),
                traspaso.getCantidad(),
                traspaso.getOrigen(),
                traspaso.getRegistradoPor(),
                traspaso.getTimestampTraspaso()
        );
    }
}
