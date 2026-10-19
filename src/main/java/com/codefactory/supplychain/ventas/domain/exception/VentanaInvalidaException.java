package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** Operación inválida sobre una ventana de quiebre (HTTP 400). */
public class VentanaInvalidaException extends ReglaDeNegocioException {
    public VentanaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
