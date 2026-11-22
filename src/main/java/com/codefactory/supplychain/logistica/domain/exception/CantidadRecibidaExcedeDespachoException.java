package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaNoProcesableException;

/**
 * La cantidad recibida supera lo despachado en la parada (HU-49). La recepción no puede
 * sumar a la Bodega_Tienda más de lo que salió del CD en ese ciclo. Responde 422.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class CantidadRecibidaExcedeDespachoException extends ReglaNoProcesableException {

    public CantidadRecibidaExcedeDespachoException(int cantidadRecibida, int cantidadDespachada) {
        super("La cantidad recibida (" + cantidadRecibida + ") supera la despachada ("
                + cantidadDespachada + ")");
    }
}
