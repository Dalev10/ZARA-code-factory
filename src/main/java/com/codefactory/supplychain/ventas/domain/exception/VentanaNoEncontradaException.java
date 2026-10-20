package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/** La ventana no existe o está fuera del alcance de tiendas del usuario (HTTP 404). */
public class VentanaNoEncontradaException extends RecursoNoEncontradoException {
    public VentanaNoEncontradaException(java.util.UUID id) {
        super("Ventana de quiebre no encontrada: " + id);
    }
}
