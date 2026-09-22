package com.codefactory.supplychain.shared.exception;

/**
 * Un recurso que debía ser único (ej. email, nombre de rol) ya existe.
 * Cualquier módulo puede extenderla para que se traduzca automáticamente a HTTP 409
 * sin que este paquete transversal dependa de ese módulo.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class RecursoDuplicadoException extends RuntimeException {

    protected RecursoDuplicadoException(String message) {
        super(message);
    }
}
