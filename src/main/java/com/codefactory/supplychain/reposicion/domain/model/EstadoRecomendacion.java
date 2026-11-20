package com.codefactory.supplychain.reposicion.domain.model;

/**
 * Estados de una recomendación de reposición al CD (D-79).
 * Transiciones: BORRADOR → APROBADA o RECHAZADA.
 *
 * Módulo: reposicion — HU-43.
 */
public enum EstadoRecomendacion {
    BORRADOR,
    APROBADA,
    RECHAZADA
}
