package com.codefactory.supplychain.inventario.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado inmutable de una operación sobre Inventario.
 *
 * La cantidad representa el delta aplicado a {@code aLaMano}: es positiva
 * cuando aumenta y negativa cuando disminuye.
 */
public record MovimientoInventario(
        UUID id,
        TipoMovimientoInventario tipo,
        int cantidad,
        String motivo,
        int saldoResultante,
        Instant ocurridoEn) {

    public MovimientoInventario {
        Objects.requireNonNull(id, "El id del movimiento es obligatorio");
        Objects.requireNonNull(tipo, "El tipo del movimiento es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del movimiento es obligatoria");
        if (motivo != null && motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo no puede estar vacío");
        }
    }
}
