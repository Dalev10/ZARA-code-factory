package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza cuando los datos de un UmbralReposicion violan una invariante de dominio
 * (alcance ni exclusivo ni presente, mínimo no positivo u objetivo no mayor al mínimo).
 *
 * Módulo: reposicion — Umbrales de reposición interna (FEAT-09).
 */
public class UmbralInvalidoException extends ReglaDeNegocioException {

    public UmbralInvalidoException(String mensaje) {
        super(mensaje);
    }
}
