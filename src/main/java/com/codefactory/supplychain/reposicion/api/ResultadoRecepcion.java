package com.codefactory.supplychain.reposicion.api;

/**
 * Resultado de registrar la recepción de un ciclo en la orden.
 *
 * @param estadoFinal       {@code "CERRADA"} si no queda pendiente o {@code "REABIERTA"} si queda
 * @param cantidadPendiente solicitado − recibido acumulado
 */
public record ResultadoRecepcion(String estadoFinal, int cantidadPendiente) {
}
