package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.InventarioMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.InventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Override
    public Set<UUID> buscarVariantesCargadas(UUID nodoId, Set<UUID> varianteIds) {
        if (varianteIds.isEmpty()) {
            return Set.of();
        }
        return repository.findByNodoIdAndVarianteIdIn(nodoId, varianteIds).stream()
                .map(InventarioJpaEntity::getVarianteId)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<Inventario> buscarPorNodo(UUID nodoId, Pageable pageable) {
        return repository.findByNodoIdOrderByVarianteIdAsc(nodoId, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Inventario> buscarPorVariante(UUID varianteId) {
        return repository.findByVarianteIdOrderByNodoIdAsc(varianteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Inventario> buscarPorVarianteEnTiendas(UUID varianteId, Set<UUID> tiendaIds) {
        return repository.buscarPorVarianteEnTiendas(varianteId, tiendaIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Inventario> buscarPorVarianteEnCds(UUID varianteId) {
        return repository.buscarPorVarianteEnCds(varianteId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
