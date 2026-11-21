package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * La cantidad recibida supera lo despachado en la parada (HU-49). La recepción no puede
 * sumar a la Bodega_Tienda más de lo que salió del CD en ese ciclo.
 * <p>
 * La ficha pide 422. El manejador global traduce {@link ReglaDeNegocioException} a 400, así
 * que el controlador de recepción la traduce a 422 con su propio {@code @ExceptionHandler}.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class CantidadRecibidaExcedeDespachoException extends ReglaDeNegocioException {

    public CantidadRecibidaExcedeDespachoException(int cantidadRecibida, int cantidadDespachada) {
        super("La cantidad recibida (" + cantidadRecibida + ") supera la despachada ("
                + cantidadDespachada + ")");
    }
}
