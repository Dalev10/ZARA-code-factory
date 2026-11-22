package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/** El viaje o la parada no están en el estado que permite la operación (HU-48, 409). */
public class ViajeEstadoInvalidoException extends ConflictoDeEstadoException {

    public ViajeEstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
