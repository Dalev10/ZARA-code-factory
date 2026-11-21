package com.codefactory.supplychain.shared.exception;

/**
 * La petición está bien formada y el recurso existe, pero una regla de negocio impide la
 * operación sobre él (por ejemplo, recibir más de lo despachado en una parada, HU-49).
 * Cualquier módulo puede extenderla para que se traduzca a HTTP 422.
 * <p>
 * Distinta de {@link ReglaDeNegocioException} (400: el dato enviado es inválido por sí mismo)
 * y de {@link ConflictoDeEstadoException} (409: el estado actual del recurso no lo permite).
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class ReglaNoProcesableException extends RuntimeException {

    protected ReglaNoProcesableException(String message) {
        super(message);
    }
}
