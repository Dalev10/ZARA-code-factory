package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/** No se puede anular dos veces la misma solicitud (HTTP 409). */
public class SolicitudYaAnuladaException extends RecursoDuplicadoException {
    public SolicitudYaAnuladaException(java.util.UUID id) {
        super("La solicitud " + id + " ya está anulada");
    }
}
