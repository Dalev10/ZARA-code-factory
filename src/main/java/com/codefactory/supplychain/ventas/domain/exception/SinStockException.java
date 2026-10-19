package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/** Sin stock suficiente (D-35). */
public class SinStockException extends ConflictoDeEstadoException {
    public SinStockException(String sku) { super("Stock insuficiente para el SKU " + sku); }
}
