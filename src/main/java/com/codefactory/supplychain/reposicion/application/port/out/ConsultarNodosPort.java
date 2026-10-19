package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto propio de reposicion para resolver nodos de una tienda.
 *
 * Módulo: reposicion.
 */
public interface ConsultarNodosPort {

    /** Id del nodo ALMACEN (piso de venta) de la tienda; vacío si no existe. */
    Optional<UUID> buscarNodoAlmacen(UUID tiendaId);

    /** Id del nodo BODEGA_TIENDA de la tienda; vacío si no existe. */
    Optional<UUID> buscarNodoBodega(UUID tiendaId);
}
