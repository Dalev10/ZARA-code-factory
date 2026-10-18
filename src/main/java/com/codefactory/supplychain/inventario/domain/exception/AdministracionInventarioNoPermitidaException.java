package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ProhibidoException;

/**
 * El usuario no tiene el scope que exige el tipo de nodo para administrar su
 * inventario (D-31).
 */
public class AdministracionInventarioNoPermitidaException extends ProhibidoException {

    public AdministracionInventarioNoPermitidaException(String scopeRequerido) {
        super("Administrar el inventario de este nodo requiere el scope " + scopeRequerido);
    }
}
