package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.InventarioMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.InventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia del agregado Inventario.
 */
@Component
@RequiredArgsConstructor
public class InventarioRepositoryAdapter implements InventarioRepositoryPort {

    private final InventarioJpaRepository repository;
    private final InventarioMapper mapper;

    @Override
    public Optional<Inventario> buscarPorNodoYVariante(UUID nodoId, UUID varianteId) {
        return repository.findByNodoIdAndVarianteId(nodoId, varianteId).map(mapper::toDomain);
    }

    @Override
    public Inventario guardar(Inventario inventario) {
        return mapper.toDomain(repository.save(mapper.toEntity(inventario)));
    }
}
