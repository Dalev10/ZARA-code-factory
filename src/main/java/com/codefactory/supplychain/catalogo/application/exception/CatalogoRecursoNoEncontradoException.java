package com.codefactory.supplychain.catalogo.application.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/**
 * Se lanza cuando se intenta consultar, modificar, eliminar o referenciar
 * una entidad del catálogo (Categoria, Template o Variante) que no existe.
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class CatalogoRecursoNoEncontradoException extends RecursoNoEncontradoException {

    private CatalogoRecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static CatalogoRecursoNoEncontradoException categoria(UUID id) {
        return new CatalogoRecursoNoEncontradoException("No existe una Categoria con id " + id);
    }

    public static CatalogoRecursoNoEncontradoException template(UUID id) {
        return new CatalogoRecursoNoEncontradoException("No existe un Template con id " + id);
    }

    public static CatalogoRecursoNoEncontradoException variante(UUID id) {
        return new CatalogoRecursoNoEncontradoException("No existe una Variante con id " + id);
    }

    public static CatalogoRecursoNoEncontradoException varianteConSku(String sku) {
        return new CatalogoRecursoNoEncontradoException("No existe una Variante con sku " + sku);
    }
}
