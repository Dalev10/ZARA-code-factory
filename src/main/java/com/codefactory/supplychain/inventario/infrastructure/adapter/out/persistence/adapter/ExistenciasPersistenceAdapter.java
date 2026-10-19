package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.inventario.application.port.out.ExistenciasRepositoryPort;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.InventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de lectura de existencias sobre la tabla {@code inventario}.
 * Las magnitudes nulas se normalizan a 0 (la columna es NOT NULL DEFAULT 0).
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Repository
@RequiredArgsConstructor
public class ExistenciasPersistenceAdapter implements ExistenciasRepositoryPort {

    private final InventarioJpaRepository repository;

    @Override
    public Optional<StockResumen> buscarPorNodoYVariante(UUID nodoId, UUID varianteId) {
        return repository.findByNodoIdAndVarianteId(nodoId, varianteId).map(ExistenciasPersistenceAdapter::toResumen);
    }

    @Override
    public List<StockResumen> listarPorNodo(UUID nodoId) {
        return repository.findByNodoId(nodoId).stream().map(ExistenciasPersistenceAdapter::toResumen).toList();
    }

    static StockResumen toResumen(InventarioJpaEntity e) {
        return new StockResumen(
                e.getNodoId(),
                e.getVarianteId(),
                valor(e.getALaMano()),
                valor(e.getDisponibleParaUso()),
                valor(e.getEntrante()),
                valor(e.getSaliente()),
                valor(e.getPronosticado()));
    }

    private static int valor(Integer v) {
        return v == null ? 0 : v;
    }
}
