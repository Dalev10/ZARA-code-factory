package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/**
 * Recurso que reposicion necesita y no existe (404). Se crea con las fábricas estáticas.
 */
public class ReposicionRecursoNoEncontradoException extends RecursoNoEncontradoException {

    private ReposicionRecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static ReposicionRecursoNoEncontradoException umbral(UUID id) {
        return new ReposicionRecursoNoEncontradoException("No existe un umbral de reposición con id " + id);
    }

    public static ReposicionRecursoNoEncontradoException variante(UUID id) {
        return new ReposicionRecursoNoEncontradoException("No existe una Variante con id " + id);
    }
}
