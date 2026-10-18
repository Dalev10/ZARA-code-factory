package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.PrioridadTiendaCdEntity;
import org.springframework.stereotype.Component;

@Component
public class PrioridadTiendaCdMapper {

    public PrioridadTiendaCdEntity toEntity(PrioridadTiendaCd p) {
        return new PrioridadTiendaCdEntity(p.getId(), p.getTiendaId(), p.getCdId(), p.getOrdenPrioridad());
    }

    public PrioridadTiendaCd toDomain(PrioridadTiendaCdEntity e) {
        return PrioridadTiendaCd.reconstruir(e.getId(), e.getTiendaId(), e.getCdId(), e.getOrdenPrioridad());
    }
}
