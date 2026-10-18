package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ProhibidoException;

public class OperacionVentaProhibidaException extends ProhibidoException {
    public OperacionVentaProhibidaException(String message) { super(message); }
}
