package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/**
 * Una venta normal o la salida de un traspaso exceden las existencias del nodo.
 * HTTP 409 (D-35).
 */
public class StockInsuficienteException extends ConflictoDeEstadoException {

    public StockInsuficienteException(int disponible, int solicitado) {
        super("Stock insuficiente: disponible=" + disponible + ", solicitado=" + solicitado);
    }
}
