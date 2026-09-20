package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class NodoInvalidoException extends ReglaDeNegocioException {

    public NodoInvalidoException(String message) {
        super(message);
    }
}
