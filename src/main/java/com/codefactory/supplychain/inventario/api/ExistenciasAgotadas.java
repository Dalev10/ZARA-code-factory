package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado cuando una venta deja las existencias disponibles en cero.
 */
public record ExistenciasAgotadas(
        UUID nodoId,
        UUID varianteId,
        Instant fecha) {
}
