package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

public class VarianteVentaNoEncontradaException extends RecursoNoEncontradoException {
    public VarianteVentaNoEncontradaException(String sku) { super("No existe una variante con SKU " + sku); }
}
