package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** Una recomendación que viola sus invariantes de dominio. */
public class RecomendacionInvalidaException extends ReglaDeNegocioException {

    public RecomendacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
