package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

/**
 * La Bodega_Tienda no tiene las unidades que pide un traspaso manual: nunca se deja en
 * negativo (D-56). HTTP 409.
 *
 * Módulo: reposicion — Reposición interna.
 */
public class BodegaInsuficienteException extends ConflictoDeEstadoException {

    public BodegaInsuficienteException(UUID varianteId, int solicitado) {
        super("La Bodega no tiene " + solicitado + " unidades de la variante " + varianteId);
    }
}
