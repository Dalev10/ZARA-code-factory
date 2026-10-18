package com.codefactory.supplychain.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolución de nodos de Almacén. Inventario no publica este contrato en su api/ (PARCIAL: ver la
 * matriz). Se necesita en ambos sentidos: tienda -&gt; Almacén (HU-34, reconciliación) y nodo -&gt; tienda
 * (listener de ExistenciasActualizadas, que solo trae nodoId).
 */
public interface AlmacenTiendaPort {

    Optional<UUID> almacenDeTienda(UUID tiendaId);

    /** Vacío si el nodo no existe o no es de tipo ALMACEN (CD y Bodega no generan ventanas). */
    Optional<UUID> tiendaDeAlmacen(UUID nodoId);
}
