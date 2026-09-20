package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.TiendaMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.TiendaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public List<Tienda> listarTodas() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }
}
