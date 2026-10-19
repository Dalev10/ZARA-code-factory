package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

public class VentaYaAnuladaException extends ConflictoDeEstadoException {

    public VentaYaAnuladaException(UUID ventaId) {
        super("La venta " + ventaId + " ya fue anulada");
    }
}
