package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Los datos de un nivel de reposición al CD violan una invariante (alcance ni exclusivo ni
 * presente, mínimo no positivo u objetivo no mayor al mínimo).
 *
 * Módulo: reposicion — Abastecimiento desde el CD (Sprint 3).
 */
public class NivelReposicionInvalidoException extends ReglaDeNegocioException {

    public NivelReposicionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
