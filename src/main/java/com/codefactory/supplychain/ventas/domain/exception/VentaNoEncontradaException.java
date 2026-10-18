package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

public class VentaNoEncontradaException extends RecursoNoEncontradoException {

    public VentaNoEncontradaException(UUID ventaId) {
        super("No se encontró la venta " + ventaId);
    }
}
