package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaNoProcesableException;

import java.util.UUID;

/** El ciclo es de una orden de otro CD: un viaje parte de un solo CD (HU-47, 422). */
public class CicloDeOtroCdException extends ReglaNoProcesableException {

    public CicloDeOtroCdException(UUID cicloId, UUID cdId) {
        super("El ciclo de despacho " + cicloId + " no sale del CD " + cdId);
    }
}
