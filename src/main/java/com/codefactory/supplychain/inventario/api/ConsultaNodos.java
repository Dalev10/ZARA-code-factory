package com.codefactory.supplychain.inventario.api;

import java.util.Optional;
import java.util.UUID;

/** Resolución de los nodos de una tienda para otros módulos (solo lectura). Consumidores: ventas y reposicion. */
public interface ConsultaNodos {

    /** Id del nodo ALMACEN (piso de venta) de la tienda; vacío si la tienda no lo tiene. */
    Optional<UUID> buscarAlmacen(UUID tiendaId);

    /** Id del nodo BODEGA_TIENDA de la tienda; vacío si la tienda no lo tiene. */
    Optional<UUID> buscarBodega(UUID tiendaId);
}
