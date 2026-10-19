package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.PrioridadTiendaCdResponse;
import org.springframework.stereotype.Component;

@Component
public class PrioridadTiendaCdWebMapper {

    public PrioridadTiendaCdResponse toResponse(PrioridadTiendaCd p) {
        return new PrioridadTiendaCdResponse(p.getId(), p.getTiendaId(), p.getCdId(), p.getOrdenPrioridad());
    }
}
