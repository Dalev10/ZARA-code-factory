package com.codefactory.supplychain.reposicion.api;

import java.util.UUID;

/**
 * Transiciones de la orden de reabastecimiento que pide logística.
 * La orden y su máquina de estados viven en reposicion; logística solo pide transiciones.
 * <p>
 * No tocan el inventario: el movimiento lo hace logística con {@code registrarDespacho}
 * y {@code registrarRecepcion} de {@code inventario/api}. Participan en la transacción
 * del llamador y no reintentan: un conflicto de versión sale al confirmar y el caso de
 * uso de entrada del consumidor reintenta fuera de la transacción.
 * <p>
 * Todas lanzan {@link OrdenNoEncontradaException} si la orden no existe y
 * {@link OrdenEstadoInvalidoException} si su estado no permite la transición.
 */
public interface TransicionesOrden {

    /** APROBADA o REABIERTA → EN_DESPACHO */
    OrdenParaDespacho iniciarDespacho(UUID ordenId);

    /** EN_DESPACHO → DESPACHADA; acumula lo despachado */
    void confirmarDespacho(UUID ordenId, int cantidadDespachada);

    /** DESPACHADA → EN_TRANSITO*/
    void marcarEnTransito(UUID ordenId);

    /**
     * EN_TRANSITO → RECIBIDA_TOTAL → CERRADA, o RECIBIDA_PARCIAL → REABIERTA; recalcula
     * el pendiente.
     */
    ResultadoRecepcion registrarRecepcion(UUID ordenId, int cantidadRecibida);
}
