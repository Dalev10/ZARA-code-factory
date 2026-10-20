package com.codefactory.supplychain.inventario.application.port.in;

/**
 * Un ítem del lote de carga inicial: SKU y cantidad absoluta (D-28, D-30).
 */
public record ItemCargaInicial(String sku, int cantidad) {
}
