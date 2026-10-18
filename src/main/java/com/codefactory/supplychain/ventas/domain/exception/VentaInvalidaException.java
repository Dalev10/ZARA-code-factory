package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

public class VentaInvalidaException extends ReglaDeNegocioException {
    public VentaInvalidaException(String message) { super(message); }
}
