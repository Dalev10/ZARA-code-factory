package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/** Una orden del viaje no está DESPACHADA, así que no puede pasar a EN_TRANSITO (HU-48, 409). */
public class OrdenNoAdmiteSalidaException extends ConflictoDeEstadoException {

    public OrdenNoAdmiteSalidaException(String mensaje) {
        super(mensaje);
    }
}
