package com.codefactory.supplychain.inventario.api;

/**
 * Estado de una reserva en un CD (ver {@link ReservasInventario#reservar}).
 */
public enum EstadoReserva {
    /** El disponible del CD cubría la cantidad: se reservó y se sumó el entrante. */
    ASIGNADO,
    /** El disponible del CD no cubría la cantidad: no se cambió nada. */
    SIN_DISPONIBLE
}
