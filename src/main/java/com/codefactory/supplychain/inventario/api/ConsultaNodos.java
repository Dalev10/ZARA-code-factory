package com.codefactory.supplychain.inventario.api;

import java.util.Optional;
import java.util.UUID;

/** Resolución de nodos de una tienda para otros módulos (solo lectura). Consumidor: ventas (HU-31). */
public interface ConsultaNodos {

    /** Id del nodo ALMACEN (piso de venta) de la tienda; vacío si la tienda no lo tiene. */
    Optional<UUID> buscarAlmacen(UUID tiendaId);
}
