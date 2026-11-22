package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/**
 * La parada no existe, no pertenece al viaje indicado o es de una tienda fuera del alcance del
 * usuario. Los tres casos responden igual (404) para no revelar que la parada existe.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class ParadaNoEncontradaException extends RecursoNoEncontradoException {

    public ParadaNoEncontradaException(UUID paradaId) {
        super("No existe la parada " + paradaId);
    }
}
