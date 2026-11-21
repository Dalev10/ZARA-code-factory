package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.Recepcion;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.UUID;

/**
 * HU-49: registrar lo recibido de una parada en la Bodega_Tienda y cerrar o reabrir la orden.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface RegistrarRecepcionUseCase {

    ResultadoRecepcion registrar(Comando comando);

    /** El usuario viaja en el comando (D-08): quien registra sale del token. */
    record Comando(UsuarioActual actor, UUID viajeId, UUID paradaId, int cantidadRecibida) {
    }

    /**
     * @param creada            {@code false} si la parada ya tenía recepción y se devuelve la original (D-91)
     * @param estadoOrden       estado de la orden tras la recepción
     * @param cantidadPendiente pendiente de la orden tras la recepción
     */
    record ResultadoRecepcion(boolean creada, Recepcion recepcion, String estadoOrden, int cantidadPendiente) {
    }
}
