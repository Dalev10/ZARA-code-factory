package com.codefactory.supplychain.catalogo.api;

import java.util.List;
import java.util.Set;

/**
 * Consulta de solo lectura de variantes por SKU, para otros módulos.
 *
 * <h2>Cómo se consume</h2>
 * El consumidor declara un puerto propio en {@code application/port/out/} y lo
 * implementa con un adaptador en {@code infrastructure/adapter/out/catalogo/},
 * único archivo que importa este paquete. Hoy lo consume inventario para
 * resolver los SKU de la carga inicial (HU-28).
 */
public interface ConsultaVariantes {

    /**
     * Busca en una sola consulta las variantes de los SKU recibidos. Devuelve
     * solo las que existen: un SKU ausente del resultado no está en el catálogo.
     */
    List<VarianteResumen> buscarPorSkus(Set<String> skus);
}
