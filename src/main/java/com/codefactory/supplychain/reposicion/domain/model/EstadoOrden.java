package com.codefactory.supplychain.reposicion.domain.model;

/**
 * Estados de la orden de reabastecimiento.
 *
 * Módulo: reposicion.
 */
public enum EstadoOrden {
    GENERADA,
    APROBADA,
    EN_DESPACHO,
    DESPACHADA,
    EN_TRANSITO,
    RECIBIDA_TOTAL,
    RECIBIDA_PARCIAL,
    REABIERTA,
    CERRADA
}
