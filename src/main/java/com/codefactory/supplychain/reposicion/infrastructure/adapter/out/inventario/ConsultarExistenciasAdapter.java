package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.inventario.api.StockResumen;

/**
 * Adaptador hacia el módulo inventario para consultar existencias actuales.
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class ConsultarExistenciasAdapter implements ConsultarExistenciasPort {

    private final ConsultaInventario consultaInventario;
    private final ConsultaNodos consultaNodos;

    public ConsultarExistenciasAdapter(ConsultaInventario consultaInventario,
                                       ConsultaNodos consultaNodos) {
        this.consultaInventario = consultaInventario;
        this.consultaNodos = consultaNodos;
    }

    @Override
    public Optional<Integer> consultarStockAlmacen(UUID tiendaId, UUID varianteId) {
        return consultaNodos.buscarNodoAlmacen(tiendaId)
                .flatMap(nodoId -> consultaInventario.consultarStock(nodoId, varianteId))
                .map(StockResumen::aLaMano);
    }

    @Override
    public Optional<Integer> consultarStockBodega(UUID tiendaId, UUID varianteId) {
        return consultaNodos.buscarNodoBodega(tiendaId)
                .flatMap(nodoId -> consultaInventario.consultarStock(nodoId, varianteId))
                .map(StockResumen::aLaMano);
    }

    @Override
    public List<UUID> variantesConInventarioEnAlmacen(UUID tiendaId) {
        return consultaNodos.buscarNodoAlmacen(tiendaId)
                .map(nodoId -> consultaInventario.consultarExistenciasPorNodo(nodoId)
                        .stream()
                        .map(StockResumen::varianteId)
                        .toList())
                .orElse(List.of());
    }
}