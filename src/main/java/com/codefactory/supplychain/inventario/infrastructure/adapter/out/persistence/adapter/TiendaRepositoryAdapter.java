package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.TiendaMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.TiendaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
@RequiredArgsConstructor
public class TiendaRepositoryAdapter implements TiendaRepositoryPort {

    private final TiendaJpaRepository jpaRepository;
    private final TiendaMapper mapper;

    @Override
    public Tienda guardar(Tienda tienda) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(tienda)));
    }

    @Override
    public Optional<Tienda> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Tienda> listarTodas(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }
}
