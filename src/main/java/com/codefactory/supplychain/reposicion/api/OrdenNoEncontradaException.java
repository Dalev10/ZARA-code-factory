package com.codefactory.supplychain.reposicion.api;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/**
 * Error de frontera: la orden de reabastecimiento no existe (sección 4.2).
 * Extiende la excepción transversal de "no encontrado", así que el manejador web
 * global la traduce a HTTP 404 sin que el consumidor haga nada.
 */
public class OrdenNoEncontradaException extends RecursoNoEncontradoException {

    public OrdenNoEncontradaException(UUID ordenId) {
        super("No existe una orden de reabastecimiento con id " + ordenId);
    }
}
