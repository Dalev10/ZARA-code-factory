package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado después de desactivar una tienda. Consumidor: ventas, que cierra
 * sus ventanas de quiebre abiertas con TIENDA_DESACTIVADA (D-50).
 */
public record TiendaDesactivada(UUID tiendaId, Instant fecha) {
}
