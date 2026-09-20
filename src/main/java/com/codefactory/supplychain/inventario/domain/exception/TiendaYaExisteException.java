package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class TiendaYaExisteException extends RecursoDuplicadoException {

    public TiendaYaExisteException() {
        super("Ya existe una tienda con ese nombre");
    }
}
