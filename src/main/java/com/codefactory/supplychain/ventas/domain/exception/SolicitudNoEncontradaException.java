package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

/** La solicitud no existe o está fuera del alcance de tiendas del usuario (HTTP 404, sin revelar existencia). */
public class SolicitudNoEncontradaException extends RecursoNoEncontradoException {
    public SolicitudNoEncontradaException(java.util.UUID id) {
        super("Solicitud no satisfecha no encontrada: " + id);
    }
}
