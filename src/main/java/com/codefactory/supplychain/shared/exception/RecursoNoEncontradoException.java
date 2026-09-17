package com.codefactory.supplychain.shared.exception;

/**
 * Un recurso solicitado por id (ej. un rol, un scope) no existe.
 * Cualquier módulo puede extenderla para que se traduzca automáticamente a HTTP 404
 * sin que este paquete transversal dependa de ese módulo.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class RecursoNoEncontradoException extends RuntimeException {

    protected RecursoNoEncontradoException(String message) {
        super(message);
    }
}
