package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/** No se puede anular dos veces la misma solicitud (HTTP 409). */
public class SolicitudYaAnuladaException extends ConflictoDeEstadoException {
    public SolicitudYaAnuladaException(java.util.UUID id) {
        super("La solicitud " + id + " ya está anulada");
    }
}
