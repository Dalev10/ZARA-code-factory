package com.codefactory.supplychain.inventario.application.port.in;

/**
 * Resultado de {@link ReservarInventarioUseCase#reservar}.
 *
 * @param asignada         si la reserva se aplicó
 * @param disponibleActual disponible del CD después de la operación
 */
public record ResultadoReservaCd(boolean asignada, int disponibleActual) {
}
