package com.codefactory.supplychain.catalogo.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Consulta de solo lectura de variantes, para otros módulos.
 *
 * <h2>Cómo se consume</h2>
 * El consumidor declara un puerto propio en {@code application/port/out/} y lo
 * implementa con un adaptador en {@code infrastructure/adapter/out/catalogo/},
 * único archivo que importa este paquete. Consumidores actuales:
 * <ul>
 *   <li>reposicion: {@link #buscar(UUID)}, para conocer la categoría de una variante.</li>
 *   <li>ventas: {@link #buscarPorSku(String)}, para resolver el SKU de una venta.</li>
 *   <li>inventario: {@link #buscarPorSkus(Set)}, para resolver los SKU de la carga inicial (HU-37).</li>
 * </ul>
 */
public interface ConsultaVariantes {

    /**
     * Busca una variante por su id. Vacío si no existe.
     */
    Optional<VarianteResumen> buscar(UUID varianteId);

    /**
     * Busca una variante por su SKU. Vacío si no existe.
     */
    Optional<VarianteResumen> buscarPorSku(String sku);

    /**
     * Busca en una sola consulta las variantes de los SKU recibidos. Devuelve
     * solo las que existen: un SKU ausente del resultado no está en el catálogo.
     */
    List<VarianteResumen> buscarPorSkus(Set<String> skus);
}
