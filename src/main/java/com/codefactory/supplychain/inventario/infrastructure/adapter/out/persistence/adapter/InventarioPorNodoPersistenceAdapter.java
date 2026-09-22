package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
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

    @Override
    public List<InventarioResumen> consultarPorNodo(UUID nodoId) {
        return repository.findByNodoId(nodoId).stream().map(this::toResumen).toList();
    }

    private InventarioResumen toResumen(InventarioJpaEntity inventario) {
        return new InventarioResumen(
                inventario.getVarianteId(), inventario.getALaMano(), inventario.getDisponibleParaUso());
    }
}
