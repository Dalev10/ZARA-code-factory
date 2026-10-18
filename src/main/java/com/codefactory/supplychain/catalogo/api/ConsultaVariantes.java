package com.codefactory.supplychain.catalogo.api;

import java.util.Optional;

/** Consulta de variantes para otros módulos (solo lectura). Consumidor: ventas (HU-31/34). */
public interface ConsultaVariantes {

    Optional<VarianteResumen> buscarPorSku(String sku);
}
