package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.UUID;

public class VentaYaAnuladaException extends RecursoDuplicadoException {

    public VentaYaAnuladaException(UUID ventaId) {
        super("La venta " + ventaId + " ya fue anulada");
    }
}
