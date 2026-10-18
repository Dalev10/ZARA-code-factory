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

    /**
     * Dado un nodoId, devuelve el tiendaId si ese nodo es de tipo ALMACEN.
     * Vacío si el nodo no existe o no es un Almacén (Bodega, CD).
     * Usado por el listener para filtrar eventos de nodos que no son Almacén.
     */
    Optional<UUID> buscarTiendaPorNodoAlmacen(UUID nodoId);
}
