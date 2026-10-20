package com.codefactory.supplychain.shared.exception;

/**
 * La operación es válida, pero choca con el estado actual del recurso (por
 * ejemplo, stock insuficiente o una venta ya anulada). Cualquier módulo puede
 * extenderla para que se traduzca a HTTP 409. Distinta de RecursoDuplicadoException,
 * que indica un valor único repetido.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
public abstract class ConflictoDeEstadoException extends RuntimeException {

    protected ConflictoDeEstadoException(String message) {
        super(message);
    }
}
