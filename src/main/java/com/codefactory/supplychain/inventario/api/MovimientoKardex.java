package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento inmutable del historial de kardex.
 */
public record MovimientoKardex(
        UUID id,
        UUID nodoId,
        UUID varianteId,
        TipoMovimientoKardex tipo,
        int cantidad,
        int saldoResultante,
        Instant fecha) {
}
