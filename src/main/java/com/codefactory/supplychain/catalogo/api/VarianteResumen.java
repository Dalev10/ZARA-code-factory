package com.codefactory.supplychain.catalogo.api;

import java.util.UUID;

/**
 * Datos de una variante que otros módulos necesitan hoy: su id y su SKU.
 */
public record VarianteResumen(UUID id, String sku) {
}
