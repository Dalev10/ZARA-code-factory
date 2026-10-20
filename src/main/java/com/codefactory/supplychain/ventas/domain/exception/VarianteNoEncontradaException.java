package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/** Producto fuera de catálogo: no se registra en este MVP (HTTP 404). */
public class VarianteNoEncontradaException extends RecursoNoEncontradoException {
    public VarianteNoEncontradaException(java.util.UUID id) {
        super("La variante " + id + " no existe en el catálogo");
    }
}
