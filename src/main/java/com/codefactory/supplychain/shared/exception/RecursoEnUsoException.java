package com.codefactory.supplychain.shared.exception;

/**
 * El recurso no puede eliminarse o modificarse porque otro registro lo está
 * usando (por ejemplo, un CD con prioridades asignadas). Se traduce a 409 Conflict.
 */
public abstract class RecursoEnUsoException extends RuntimeException {

    protected RecursoEnUsoException(String message) {
        super(message);
    }
}
