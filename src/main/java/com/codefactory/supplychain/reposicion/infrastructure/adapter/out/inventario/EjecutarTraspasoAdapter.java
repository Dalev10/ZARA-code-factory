package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia inventario.api: ejecuta el traspaso Bodega → Almacén registrando el
 * movimiento en el kardex de inventario.
 *
 * Módulo: reposicion.
 */
@Component
public class EjecutarTraspasoAdapter implements EjecutarTraspasoPort {

    private final InventarioMovimientos inventarioMovimientos;
    private final ConsultarNodosPort consultarNodos;

    public EjecutarTraspasoAdapter(InventarioMovimientos inventarioMovimientos,
                                   ConsultarNodosPort consultarNodos) {
        this.inventarioMovimientos = inventarioMovimientos;
        this.consultarNodos = consultarNodos;
    }

    @Override
    public void traspasar(UUID tiendaId, UUID varianteId, int cantidad, UUID traspasoId) {
        UUID nodoOrigen = consultarNodos.buscarNodoBodega(tiendaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Bodega no encontrada para tienda " + tiendaId));
        UUID nodoDestino = consultarNodos.buscarNodoAlmacen(tiendaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Almacén no encontrado para tienda " + tiendaId));
        inventarioMovimientos.trasladar(nodoOrigen, nodoDestino, varianteId, cantidad, traspasoId);
    }
}
