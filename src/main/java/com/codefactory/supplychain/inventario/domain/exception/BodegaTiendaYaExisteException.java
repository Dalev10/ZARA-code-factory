package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class BodegaTiendaYaExisteException extends RecursoDuplicadoException {

    public BodegaTiendaYaExisteException() {
        super("Ya existe una bodega para la tienda indicada");
    }
}
