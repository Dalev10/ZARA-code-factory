package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.mapper;

import org.springframework.stereotype.Component;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.BodegaTiendaEntity;

/**
 * Convierte bodegas de tienda entre el dominio y JPA.
 */
@Component
public class BodegaTiendaPersistenceMapper {

    public BodegaTiendaEntity toEntity(BodegaTienda bodegaTienda) {
        return new BodegaTiendaEntity(bodegaTienda.getId(), bodegaTienda.getTiendaId());
    }

    public BodegaTienda toDomain(BodegaTiendaEntity entity) {
        return BodegaTienda.reconstruir(entity.getId(), entity.getTiendaId());
    }
}
