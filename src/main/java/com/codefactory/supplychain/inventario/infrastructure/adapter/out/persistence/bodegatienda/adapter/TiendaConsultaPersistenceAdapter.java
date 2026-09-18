package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.adapter;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaConsultaPort;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.TiendaJpaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository.TiendaJpaRepository;

/**
 * Adaptador TEMPORAL y provisional para comprobar y consultar tiendas
 * asociadas a BodegaTienda.
 *
 * No implementa la persistencia oficial del módulo Tienda.
 */
@Repository
public class TiendaConsultaPersistenceAdapter implements TiendaConsultaPort {

    private final TiendaJpaRepository repository;

    public TiendaConsultaPersistenceAdapter(TiendaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existe(Long tiendaId) {
        return repository.existsById(tiendaId);
    }

    @Override
    public Optional<TiendaInfo> buscarInfo(Long tiendaId) {
        return repository.findById(tiendaId).map(this::toInfo);
    }

    private TiendaInfo toInfo(TiendaJpaEntity tienda) {
        return new TiendaInfo(tienda.getId(), tienda.getNombre(), tienda.getUbicacion());
    }
}
