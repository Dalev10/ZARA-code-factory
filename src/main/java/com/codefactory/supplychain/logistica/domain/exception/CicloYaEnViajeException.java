package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

/** El ciclo de despacho ya está en otro viaje (HU-47, 409). */
public class CicloYaEnViajeException extends ConflictoDeEstadoException {

    public CicloYaEnViajeException(UUID cicloId) {
        super("El ciclo de despacho " + cicloId + " ya está en otro viaje");
    }
}
