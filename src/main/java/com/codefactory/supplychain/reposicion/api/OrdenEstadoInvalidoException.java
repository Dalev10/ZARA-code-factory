package com.codefactory.supplychain.reposicion.api;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/**
 * Error de frontera: la orden no está en un estado que permita la transición pedida. 
 * Extiende la excepción transversal de conflicto de estado, así que el
 * manejador web global la traduce a HTTP 409.
 */
public class OrdenEstadoInvalidoException extends ConflictoDeEstadoException {

    public OrdenEstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
