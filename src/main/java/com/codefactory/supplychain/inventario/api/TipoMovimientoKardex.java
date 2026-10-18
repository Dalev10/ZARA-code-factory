package com.codefactory.supplychain.inventario.api;

/**
 * Tipos de movimiento que pueden aparecer en el kardex.
 */
public enum TipoMovimientoKardex {
    CARGA_INICIAL,
    AJUSTE,
    VENTA,
    ANULACION_VENTA,
    REPOSICION,
    TRASPASO_SALIDA,
    TRASPASO_ENTRADA
}
