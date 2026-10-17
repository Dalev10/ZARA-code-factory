package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Indica que una operación o estado de Inventario viola una regla de negocio.
 */
public class InventarioInvalidoException extends ReglaDeNegocioException {

    public InventarioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
