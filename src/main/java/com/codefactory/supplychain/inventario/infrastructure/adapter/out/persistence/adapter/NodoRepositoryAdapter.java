package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.NodoMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.NodoJpaRepository;
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
public class NodoRepositoryAdapter implements NodoRepositoryPort {

    private final NodoJpaRepository jpaRepository;
    private final NodoMapper mapper;

    @Override
    public Nodo guardar(Nodo nodo) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(nodo)));
    }

    @Override
    public Optional<Nodo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Nodo> buscarPorCdId(UUID cdId) {
        return jpaRepository.findByCdId(cdId).map(mapper::toDomain);
    }

    @Override
    public Optional<Nodo> buscarPorTiendaYTipo(UUID tiendaId, TipoNodo tipo) {
        return jpaRepository.findByTiendaIdAndTipo(tiendaId, tipo).map(mapper::toDomain);
    }

    @Override
    public List<Nodo> listarPorTipo(TipoNodo tipo) {
        return jpaRepository.findByTipo(tipo).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorTiendaYTipo(UUID tiendaId, TipoNodo tipo) {
        return jpaRepository.existsByTiendaIdAndTipo(tiendaId, tipo);
    }

    @Override
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
