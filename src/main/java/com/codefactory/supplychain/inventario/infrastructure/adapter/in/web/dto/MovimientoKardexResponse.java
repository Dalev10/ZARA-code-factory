package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;

import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento del kardex de un nodo. No incluye saldo resultante: el esquema
 * actual no lo persiste.
 */
public record MovimientoKardexResponse(
        UUID id,
        UUID varianteId,
        String tipo,
        int cantidad,
        String motivo,
        String comentario,
        boolean forzado,
        UUID usuarioId,
        Instant ocurridoEn) {

    public static MovimientoKardexResponse desde(MovimientoKardexConsulta movimiento) {
        return new MovimientoKardexResponse(
                movimiento.id(),
                movimiento.varianteId(),
                movimiento.tipo().name(),
                movimiento.cantidad(),
                movimiento.motivo(),
                movimiento.comentario(),
                movimiento.forzado(),
                movimiento.usuarioId(),
                movimiento.ocurridoEn());
    }
}
