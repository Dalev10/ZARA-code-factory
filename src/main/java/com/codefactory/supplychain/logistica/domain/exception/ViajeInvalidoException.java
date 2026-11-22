package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** El viaje o su registro no cumplen una regla de forma (sin paradas, orden repetido, hora futura...): 400. */
public class ViajeInvalidoException extends ReglaDeNegocioException {

    public ViajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
