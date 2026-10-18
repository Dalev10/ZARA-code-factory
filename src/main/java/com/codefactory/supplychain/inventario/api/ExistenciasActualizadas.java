package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado después de actualizar las existencias de una variante en
 * un nodo. Es el único evento de existencias (D-20): cada consumidor decide
 * con estos datos sin volver a consultar a inventario.
 *
 * {@code tiendaId} es {@code null} cuando el nodo es un CD.
 * {@code aLaManoAnterior} y {@code aLaMano} son el saldo antes y después del
 * movimiento.
 */
public record ExistenciasActualizadas(
        UUID nodoId,
        TipoNodoInventario tipoNodo,
        UUID tiendaId,
        UUID varianteId,
        UUID movimientoId,
        TipoMovimientoKardex tipoMovimiento,
        boolean forzado,
        UUID referenciaId,
        int aLaManoAnterior,
        int aLaMano,
        int disponibleParaUso,
        int entrante,
        int saliente,
        int pronosticado,
        Instant ocurridoEn) {
}
