package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.InventarioMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.InventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador TEMPORAL y provisional de lectura sobre la tabla inventario.
 * Puede devolver siempre una lista vacía si todavía no existen registros
 * (la captura real de Inventario pertenece al Sprint 2).
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Repository
@RequiredArgsConstructor
public class InventarioPorNodoPersistenceAdapter implements InventarioPorNodoPort {

    private final InventarioJpaRepository repository;
    private final InventarioMapper mapper;

    @Override
    public List<InventarioResumen> consultarPorNodo(UUID nodoId) {
        return repository.findByNodoId(nodoId).stream()
                .map(mapper::toDomain)
                .map(inventario -> new InventarioResumen(
                        inventario.getVarianteId(),
                        inventario.getALaMano(),
                        inventario.getDisponible()))
                .toList();
    }
}
