package com.codefactory.supplychain.inventario.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado inmutable de una operación sobre Inventario.
 *
 * La cantidad representa el delta aplicado a {@code aLaMano}: es positiva
 * cuando aumenta y negativa cuando disminuye.
 *
 * {@code forzado} solo puede ser verdadero en una venta que saltó el chequeo
 * de stock suficiente.
 */
public record MovimientoInventario(
        UUID id,
        TipoMovimientoInventario tipo,
        int cantidad,
        String motivo,
        boolean forzado,
        int saldoResultante,
        Instant ocurridoEn,
        UUID referenciaId) {

    public MovimientoInventario(UUID id, TipoMovimientoInventario tipo, int cantidad, String motivo,
                                boolean forzado, int saldoResultante, Instant ocurridoEn) {
        this(id, tipo, cantidad, motivo, forzado, saldoResultante, ocurridoEn, null);
    }

    public MovimientoInventario {
        Objects.requireNonNull(id, "El id del movimiento es obligatorio");
        Objects.requireNonNull(tipo, "El tipo del movimiento es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del movimiento es obligatoria");
        if (motivo != null && motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo no puede estar vacío");
        }
        if (forzado && tipo != TipoMovimientoInventario.VENTA) {
            throw new IllegalArgumentException("Solo una venta puede ser forzada");
        }
    }

    public MovimientoInventario conReferencia(UUID referencia) {
        return new MovimientoInventario(id, tipo, cantidad, motivo, forzado, saldoResultante, ocurridoEn, referencia);
    }
}
