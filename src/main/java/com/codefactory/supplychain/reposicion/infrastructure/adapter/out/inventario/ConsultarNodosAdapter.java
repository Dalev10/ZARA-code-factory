package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * ACL hacia inventario.api: resuelve el Almacén y la Bodega de una tienda con {@link ConsultaNodos}.
 *
 * Módulo: reposicion.
 */
@Component
public class ConsultarNodosAdapter implements ConsultarNodosPort {

    private final ConsultaNodos consultaNodos;

    public ConsultarNodosAdapter(ConsultaNodos consultaNodos) {
        this.consultaNodos = consultaNodos;
    }

    @Override
    public Optional<UUID> buscarNodoAlmacen(UUID tiendaId) {
        return consultaNodos.buscarAlmacen(tiendaId);
    }

    @Override
    public Optional<UUID> buscarNodoBodega(UUID tiendaId) {
        return consultaNodos.buscarBodega(tiendaId);
    }
}
