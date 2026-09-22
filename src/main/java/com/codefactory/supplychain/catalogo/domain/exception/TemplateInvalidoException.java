package com.codefactory.supplychain.catalogo.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza cuando los datos de un Template violan una invariante de
 * dominio (nombre vacío/demasiado largo, precio base negativo, o sin
 * Categoria asociada).
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class TemplateInvalidoException extends ReglaDeNegocioException {

    public TemplateInvalidoException(String mensaje) {
        super(mensaje);
    }
}
