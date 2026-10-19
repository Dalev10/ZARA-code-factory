package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado después de actualizar las existencias de una variante en
 * un nodo.
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
