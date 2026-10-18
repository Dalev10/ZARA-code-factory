package com.codefactory.supplychain.reposicion.domain.exception;

/**
 * Se lanza cuando los datos de un traspaso interno violan las reglas de dominio.
 *
 * Módulo: reposicion — Reposición interna.
 */
public class TraspasoInvalidoException extends RuntimeException {

    public TraspasoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
