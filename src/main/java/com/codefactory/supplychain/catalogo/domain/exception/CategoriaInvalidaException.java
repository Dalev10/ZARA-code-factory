package com.codefactory.supplychain.catalogo.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza cuando los datos de una Categoria violan una invariante de
 * dominio (nombre vacío, null, o que excede la longitud permitida).
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class CategoriaInvalidaException extends ReglaDeNegocioException {

    public CategoriaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
