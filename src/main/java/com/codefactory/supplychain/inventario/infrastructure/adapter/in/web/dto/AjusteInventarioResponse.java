package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;

import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento AJUSTE registrado y el saldo resultante.
 */
public record AjusteInventarioResponse(
        UUID movimientoId,
        UUID nodoId,
        String sku,
        int delta,
        String motivo,
        String comentario,
        int aLaMano,
        Instant ocurridoEn) {

    public static AjusteInventarioResponse desde(UUID nodoId, String sku, MovimientoInventario movimiento) {
        return new AjusteInventarioResponse(
                movimiento.id(),
                nodoId,
                sku,
                movimiento.cantidad(),
                movimiento.motivo(),
                movimiento.comentario(),
                movimiento.saldoResultante(),
                movimiento.ocurridoEn());
    }
}
