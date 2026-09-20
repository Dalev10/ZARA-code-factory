package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.NodoEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
public class NodoMapper {

    public NodoEntity toEntity(Nodo nodo) {
        return NodoEntity.builder()
                .id(nodo.getId())
                .tipo(nodo.getTipo())
                .cdId(nodo.getCdId())
                .tiendaId(nodo.getTiendaId())
                .build();
    }

    public Nodo toDomain(NodoEntity entity) {
        return Nodo.reconstruir(entity.getId(), entity.getTipo(), entity.getCdId(), entity.getTiendaId());
    }
}
