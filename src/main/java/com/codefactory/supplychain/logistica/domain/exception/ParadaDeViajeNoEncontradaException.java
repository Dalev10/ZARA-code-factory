package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

import java.util.UUID;

/** La parada no existe en ese viaje (HU-48, 404). */
public class ParadaDeViajeNoEncontradaException extends RecursoNoEncontradoException {

    public ParadaDeViajeNoEncontradaException(UUID viajeId, UUID paradaId) {
        super("El viaje " + viajeId + " no tiene la parada " + paradaId);
    }
}
