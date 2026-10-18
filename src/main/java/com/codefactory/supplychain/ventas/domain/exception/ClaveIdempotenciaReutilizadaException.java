package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/** La clave de idempotencia existe pero con otros datos u otro autor (HTTP 409). */
public class ClaveIdempotenciaReutilizadaException extends RecursoDuplicadoException {
    public ClaveIdempotenciaReutilizadaException() {
        super("La clave de idempotencia ya fue usada con una solicitud distinta");
    }
}
