package com.codefactory.supplychain.catalogo.api;

import java.util.UUID;

/**
 * Lo que otros módulos pueden saber de una variante: su identidad, su SKU y la categoría
 * de su template. Published Language de catalogo: nunca se expone el dominio.
 */
public record VarianteResumen(UUID id, String sku, UUID categoriaId) {
}
