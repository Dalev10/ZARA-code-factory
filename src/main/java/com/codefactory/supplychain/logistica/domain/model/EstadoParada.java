package com.codefactory.supplychain.logistica.domain.model;

/** PENDIENTE → EN_TRANSITO (salida del viaje) → ENTREGADO (llegada a la tienda), como en V1. */
public enum EstadoParada {
    PENDIENTE,
    EN_TRANSITO,
    ENTREGADO
}
