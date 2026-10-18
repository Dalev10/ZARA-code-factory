package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/**
 * Indica que el nodo sobre el que se quiere operar inventario no existe.
 */
public class NodoNoEncontradoException extends RecursoNoEncontradoException {

    public NodoNoEncontradoException(UUID nodoId) {
        super("El nodo " + nodoId + " no existe");
    }
}
