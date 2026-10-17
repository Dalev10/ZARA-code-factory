package com.codefactory.supplychain.inventario.api;

/**
 * Error de frontera que indica que no hay existencias disponibles suficientes
 * para completar un descuento.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException() {
        super("No hay stock suficiente para completar la operación");
    }
}
