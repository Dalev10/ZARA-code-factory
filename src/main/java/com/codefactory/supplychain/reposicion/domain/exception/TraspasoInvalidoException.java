package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza cuando los datos de un traspaso interno violan las reglas de dominio. HTTP 400.
 *
 * Módulo: reposicion — Reposición interna.
 */
public class TraspasoInvalidoException extends ReglaDeNegocioException {

    public TraspasoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
