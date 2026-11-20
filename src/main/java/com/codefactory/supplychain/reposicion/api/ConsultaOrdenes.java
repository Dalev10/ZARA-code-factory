package com.codefactory.supplychain.reposicion.api;

import java.util.UUID;

/**
 * Consulta de órdenes de reabastecimiento para otros módulos (sección 4.2).
 * Solo lectura. Consumidor: logistica (HU-46, HU-47, HU-49).
 *
 * <h2>Cómo se consume</h2>
 * El consumidor declara un puerto propio en {@code application/port/out/} y lo
 * implementa con un adaptador en {@code infrastructure/adapter/out/reposicion/},
 * único archivo que importa este paquete.
 */
public interface ConsultaOrdenes {

    /**
     * @throws OrdenNoEncontradaException si la orden no existe
     */
    OrdenResumen consultarOrden(UUID ordenId);
}
