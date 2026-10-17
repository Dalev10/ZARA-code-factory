package com.codefactory.supplychain.inventario.domain.model;

/**
 * Tipos de movimiento reconocidos por el agregado Inventario.
 */
public enum TipoMovimientoInventario {
    CARGA_INICIAL,
    AJUSTE,
    VENTA,
    ANULACION_VENTA,
    REPOSICION,
    TRASPASO_SALIDA,
    TRASPASO_ENTRADA
}
