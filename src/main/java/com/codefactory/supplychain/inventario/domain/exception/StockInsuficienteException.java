package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Indica que una venta normal excede las existencias disponibles.
 */
public class StockInsuficienteException extends ReglaDeNegocioException {

    public StockInsuficienteException(int disponible, int solicitado) {
        super("Stock insuficiente: disponible=" + disponible + ", solicitado=" + solicitado);
    }
}
