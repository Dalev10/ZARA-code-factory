package com.codefactory.supplychain.catalogo.api;

import java.util.UUID;

/** Lo que otros módulos pueden saber de una variante. */
public record VarianteResumen(UUID id, String sku) {
}
