package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaNoProcesableException;

/** La hora contradice la secuencia despacho → salida → llegada (HU-48, 422). */
public class CronologiaViajeInvalidaException extends ReglaNoProcesableException {

    public CronologiaViajeInvalidaException(String mensaje) {
        super(mensaje);
    }
}
