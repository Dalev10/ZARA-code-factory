package com.codefactory.supplychain.inventario.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resuelve SKU en ids de variante. Un SKU ausente del resultado no existe en
 * el catálogo.
 */
public interface VariantesCatalogoPort {

    Map<String, UUID> resolverIdsPorSku(Set<String> skus);
}
