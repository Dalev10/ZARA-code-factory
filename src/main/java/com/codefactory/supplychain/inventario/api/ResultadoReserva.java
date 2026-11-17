package com.codefactory.supplychain.inventario.api;

import java.util.Objects;

/**
 * Resultado de {@link ReservasInventario#reservar}.
 *
 * @param estado            si la reserva se aplicó o el CD no tenía disponible
 * @param disponibleActual  disponible del CD después de la operación; con
 *                          {@link EstadoReserva#SIN_DISPONIBLE} es el que impidió
 *                          reservar (sirve para el motivo del intento de CD)
 */
public record ResultadoReserva(EstadoReserva estado, int disponibleActual) {

    public ResultadoReserva {
        Objects.requireNonNull(estado, "estado es obligatorio");
    }

    public static ResultadoReserva asignado(int disponibleActual) {
        return new ResultadoReserva(EstadoReserva.ASIGNADO, disponibleActual);
    }

    public static ResultadoReserva sinDisponible(int disponibleActual) {
        return new ResultadoReserva(EstadoReserva.SIN_DISPONIBLE, disponibleActual);
    }

    public boolean asignada() {
        return estado == EstadoReserva.ASIGNADO;
    }
}
