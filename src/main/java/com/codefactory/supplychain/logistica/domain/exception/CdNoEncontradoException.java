package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/** No existe un centro de distribución con ese id (404). */
public class CdNoEncontradoException extends RecursoNoEncontradoException {

    public CdNoEncontradoException(UUID cdId) {
        super("No existe un centro de distribución con id " + cdId);
    }
}
