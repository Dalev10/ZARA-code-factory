package com.codefactory.supplychain.shared.exception;

/**
 * El solicitante está autenticado y tiene el scope, pero una regla de negocio le impide
 * operar sobre este recurso concreto (por ejemplo, una tienda que no tiene asignada).
 * Cualquier módulo puede extenderla para que se traduzca a HTTP 403 sin que este paquete
 * transversal dependa de ese módulo. Distinta de NoAutorizadoException (401: no se sabe
 * quién es el solicitante).
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class ProhibidoException extends RuntimeException {

    protected ProhibidoException(String message) {
        super(message);
    }
}
