package com.codefactory.supplychain.catalogo.application.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Se lanza al intentar crear una Categoria con un nombre que ya pertenece a
 * otra Categoria existente, o al modificar una Categoria asignándole un
 * nombre que ya usa otra Categoria distinta de ella misma.
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class CategoriaYaExisteException extends RecursoDuplicadoException {

    public CategoriaYaExisteException(String nombre) {
        super("Ya existe una Categoria con el nombre " + nombre);
    }
}
