package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** Dato inválido al registrar una solicitud no satisfecha (HTTP 400). */
public class SolicitudInvalidaException extends ReglaDeNegocioException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
