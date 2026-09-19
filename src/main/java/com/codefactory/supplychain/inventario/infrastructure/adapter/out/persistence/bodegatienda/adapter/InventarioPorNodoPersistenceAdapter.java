package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.InventarioJpaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository.InventarioJpaRepository;

/**
 * Adaptador TEMPORAL y provisional de lectura sobre la tabla inventario.
 *
 * La captura y gestión de Inventario pertenece al Sprint 2, por lo que hoy
 * puede devolver siempre una lista vacía si todavía no existen registros.
 */
@Repository
public class InventarioPorNodoPersistenceAdapter implements InventarioPorNodoPort {

    private final InventarioJpaRepository repository;

    public InventarioPorNodoPersistenceAdapter(InventarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InventarioResumen> consultarPorNodo(Long nodoId) {
        return repository.findByNodoId(nodoId).stream()
                .map(this::toResumen)
                .toList();
    }

    private InventarioResumen toResumen(InventarioJpaEntity inventario) {
        return new InventarioResumen(
                inventario.getVarianteId(),
                inventario.getALaMano(),
                inventario.getDisponibleParaUso());
    }
}
