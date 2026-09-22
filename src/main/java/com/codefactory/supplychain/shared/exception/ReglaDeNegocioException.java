package com.codefactory.supplychain.shared.exception;

/**
 * Violación de una regla de negocio del dominio (dato inválido, invariante no cumplida).
 * Cualquier módulo puede extenderla para que sus excepciones de dominio se traduzcan
 * automáticamente a HTTP 400 sin que este paquete transversal dependa de ese módulo.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class ReglaDeNegocioException extends RuntimeException {

    protected ReglaDeNegocioException(String message) {
        super(message);
    }
}
