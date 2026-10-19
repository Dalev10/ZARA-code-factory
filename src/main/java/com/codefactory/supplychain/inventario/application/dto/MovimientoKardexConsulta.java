package com.codefactory.supplychain.inventario.application.dto;

import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;

import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento del kardex tal como lo muestra la consulta HTTP (HU-39).
 */
public record MovimientoKardexConsulta(
        UUID id,
        UUID varianteId,
        TipoMovimientoInventario tipo,
        int cantidad,
        String motivo,
        String comentario,
        boolean forzado,
        UUID usuarioId,
        Instant ocurridoEn) {
}
