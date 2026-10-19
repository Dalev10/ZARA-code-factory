package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/** Sin stock suficiente (D-35). Extiende RecursoDuplicadoException solo para mapearse a HTTP 409. */
public class SinStockException extends RecursoDuplicadoException {
    public SinStockException(String sku) { super("Stock insuficiente para el SKU " + sku); }
}
