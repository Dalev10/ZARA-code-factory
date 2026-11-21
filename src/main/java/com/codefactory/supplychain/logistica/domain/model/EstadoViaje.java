package com.codefactory.supplychain.logistica.domain.model;

/** ARMADO → EN_CURSO (salida, HU-48) → CERRADO (cuando todas las paradas están entregadas). */
public enum EstadoViaje {
    ARMADO,
    EN_CURSO,
    CERRADO
}
