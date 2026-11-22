package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado después de actualizar las existencias de una variante en
 * un nodo.
 *
 * También se publica cuando solo cambian {@code entrante} o {@code saliente}
 * (reservar y liberar, D-100). Esos cambios no pasan por el kardex (D-93), así
 * que en ese caso {@code movimientoId} y {@code tipoMovimiento} son
 * {@code null} y {@code aLaManoAnterior} es igual a {@code aLaMano}. Los
 * listeners deben tolerarlo.
 *
 * Orden de los listeners (D-99): ventas {@code @Order(10)}, traspaso automático
 * {@code @Order(20)}, recomendación {@code @Order(30)}.
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
