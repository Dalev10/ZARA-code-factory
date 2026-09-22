package com.codefactory.supplychain.shared.exception;

/**
 * El solicitante no está autenticado o sus credenciales no son válidas.
 * Cualquier módulo puede extenderla para que se traduzca automáticamente a
 * HTTP 401 sin que este paquete transversal dependa de ese módulo.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class NoAutorizadoException extends RuntimeException {

    protected NoAutorizadoException(String message) {
        super(message);
    }
}
