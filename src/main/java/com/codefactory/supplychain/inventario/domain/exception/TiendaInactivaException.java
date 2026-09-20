package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class TiendaInactivaException extends ReglaDeNegocioException {

    public TiendaInactivaException() {
        super("La tienda está inactiva");
    }
}
