package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** Una orden de reabastecimiento que viola sus invariantes de dominio. */
public class OrdenInvalidaException extends ReglaDeNegocioException {

    public OrdenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
