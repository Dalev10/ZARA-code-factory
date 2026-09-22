package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class TiendaNoEncontradaException extends RecursoNoEncontradoException {

    public TiendaNoEncontradaException() {
        super("La tienda solicitada no existe");
    }
}
