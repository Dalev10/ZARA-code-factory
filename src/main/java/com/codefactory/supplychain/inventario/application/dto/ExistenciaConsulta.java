package com.codefactory.supplychain.inventario.application.dto;

import com.codefactory.supplychain.inventario.domain.model.Inventario;

/**
 * Existencias de una variante en un nodo, con el SKU que resuelve catalogo (HD-07).
 * {@code sku} es nulo solo si la variante ya no está en el catálogo.
 */
public record ExistenciaConsulta(Inventario inventario, String sku) {
}
