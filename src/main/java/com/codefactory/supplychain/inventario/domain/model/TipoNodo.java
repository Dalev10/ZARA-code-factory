package com.codefactory.supplychain.inventario.domain.model;

/**
 * Representa los tipos de nodo polimórficos que conforman la red de suministro:
 * - {@code CD}: Centro de Distribución principal o regional.
 * - {@code BODEGA_TIENDA}: Bodega de almacenamiento interno de una tienda
 * física.
 * - {@code ALMACEN}: Piso de venta / exhibición de cara al cliente en la tienda
 * física.
 */
public enum TipoNodo {
    CD,
    BODEGA_TIENDA,
    ALMACEN
}
