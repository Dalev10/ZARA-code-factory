package com.codefactory.supplychain.inventario.api;

/**
 * Tipos de movimiento que pueden aparecer en el kardex.
 */
public enum TipoMovimientoKardex {
    CARGA_INICIAL,
    AJUSTE,
    VENTA,
    ANULACION_VENTA,
    TRASPASO_SALIDA,
    TRASPASO_ENTRADA,
    /** Salida física de un CD por un ciclo de despacho (HU-46). */
    DESPACHO,
    /** Entrada en la Bodega_Tienda por una recepción (HU-49). */
    RECEPCION
}
