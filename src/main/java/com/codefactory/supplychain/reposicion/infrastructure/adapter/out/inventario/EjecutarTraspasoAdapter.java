package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.inventario.api.ConsultaNodos;

/**
 * Adaptador que aplica el movimiento contable del traspaso en el módulo inventario.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class EjecutarTraspasoAdapter implements EjecutarTraspasoPort {

    private final InventarioMovimientos inventarioMovimientos;
    private final ConsultaNodos consultaNodos;

    public EjecutarTraspasoAdapter(InventarioMovimientos inventarioMovimientos,
                                    ConsultaNodos consultaNodos) {
        this.inventarioMovimientos = inventarioMovimientos;
        this.consultaNodos = consultaNodos;
    }

    @Override
    public void traspasar(UUID tiendaId, UUID varianteId, int cantidad, UUID traspasoId) {
        UUID nodoOrigen = consultaNodos.buscarNodoBodega(tiendaId)
                .orElseThrow(() -> new IllegalStateException("Bodega no encontrada para tienda " + tiendaId));
        UUID nodoDestino = consultaNodos.buscarNodoAlmacen(tiendaId)
                .orElseThrow(() -> new IllegalStateException("Almacén no encontrado para tienda " + tiendaId));
        inventarioMovimientos.trasladar(nodoOrigen, nodoDestino, varianteId, cantidad, traspasoId);
    }
}