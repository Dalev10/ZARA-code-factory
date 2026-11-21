package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/** No existe un ciclo de despacho con ese id (404). */
public class CicloDespachoNoEncontradoException extends RecursoNoEncontradoException {

    public CicloDespachoNoEncontradoException(UUID cicloId) {
        super("No existe un ciclo de despacho con id " + cicloId);
    }
}
