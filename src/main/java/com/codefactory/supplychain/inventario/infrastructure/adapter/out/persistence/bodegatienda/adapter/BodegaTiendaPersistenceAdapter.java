package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.BodegaTiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.BodegaTiendaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.mapper.BodegaTiendaPersistenceMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository.BodegaTiendaJpaRepository;

/**
 * Adaptador que conecta el puerto de bodegas de tienda con Spring Data JPA.
 */
@Repository
public class BodegaTiendaPersistenceAdapter implements BodegaTiendaRepositoryPort {

    private static final TipoNodo TIPO_BODEGA_TIENDA = TipoNodo.BODEGA_TIENDA;

    private final BodegaTiendaJpaRepository repository;
    private final BodegaTiendaPersistenceMapper mapper;

    public BodegaTiendaPersistenceAdapter(BodegaTiendaJpaRepository repository,
            BodegaTiendaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BodegaTienda guardar(BodegaTienda bodegaTienda) {
        BodegaTiendaEntity entity = mapper.toEntity(bodegaTienda);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<BodegaTienda> buscarPorId(Long id) {
        return repository.findByIdAndTipo(id, TIPO_BODEGA_TIENDA).map(mapper::toDomain);
    }

    @Override
    public Optional<BodegaTienda> buscarPorTiendaId(Long tiendaId) {
        return repository.findByTiendaIdAndTipo(tiendaId, TIPO_BODEGA_TIENDA).map(mapper::toDomain);
    }

    @Override
    public boolean existePorId(Long id) {
        return repository.existsByIdAndTipo(id, TIPO_BODEGA_TIENDA);
    }

    @Override
    public boolean existePorTiendaId(Long tiendaId) {
        return repository.existsByTiendaIdAndTipo(tiendaId, TIPO_BODEGA_TIENDA);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteByIdAndTipo(id, TIPO_BODEGA_TIENDA);
    }
}
