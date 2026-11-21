package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/** No existe un viaje con ese id (404). */
public class ViajeNoEncontradoException extends RecursoNoEncontradoException {

    public ViajeNoEncontradoException(UUID viajeId) {
        super("No existe un viaje con id " + viajeId);
    }
}
