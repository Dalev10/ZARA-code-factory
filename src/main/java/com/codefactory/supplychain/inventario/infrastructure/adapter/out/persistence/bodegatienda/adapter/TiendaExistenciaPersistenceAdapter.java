package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.adapter;

import org.springframework.stereotype.Repository;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaExistePort;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository.TiendaExistenciaJpaRepository;

/**
 * Adaptador provisional para comprobar tiendas asociadas a BodegaTienda.
 *
 * No implementa la persistencia oficial del módulo Tienda.
 */
@Repository
public class TiendaExistenciaPersistenceAdapter implements TiendaExistePort {

    private final TiendaExistenciaJpaRepository repository;

    public TiendaExistenciaPersistenceAdapter(TiendaExistenciaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existeTienda(Long tiendaId) {
        return repository.existsById(tiendaId);
    }
}
