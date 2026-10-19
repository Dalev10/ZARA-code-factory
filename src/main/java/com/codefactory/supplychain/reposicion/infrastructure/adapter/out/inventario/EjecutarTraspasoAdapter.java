package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
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
    public boolean traspasar(UUID tiendaId, UUID varianteId, int cantidad, UUID traspasoId) {
        UUID nodoOrigen = consultarNodos.buscarNodoBodega(tiendaId)
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.tienda(tiendaId));
        UUID nodoDestino = consultarNodos.buscarNodoAlmacen(tiendaId)
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.tienda(tiendaId));
        try {
            inventarioMovimientos.trasladar(nodoOrigen, nodoDestino, varianteId, cantidad, traspasoId);
            return true;
        } catch (StockInsuficienteException e) {
            return false;
        }
    }
}
