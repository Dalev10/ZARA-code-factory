package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

/**
 * La recomendación ya no está en BORRADOR: no se puede aprobar ni rechazar. HTTP 409.
 *
 * Módulo: reposicion.
 */
public class RecomendacionYaResueltaException extends ConflictoDeEstadoException {

    public RecomendacionYaResueltaException(UUID recomendacionId, EstadoRecomendacion estadoActual, String operacion) {
        super("No se puede " + operacion + " la recomendación " + recomendacionId + ": está en estado " + estadoActual);
    }
}
