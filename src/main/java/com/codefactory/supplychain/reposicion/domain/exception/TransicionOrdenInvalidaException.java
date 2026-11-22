package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;
import lombok.Getter;

import java.util.UUID;

/**
 * La orden no está en un estado que permita la transición pedida. HTTP 409.
 * Hacia otros módulos se traduce a {@code reposicion.api.OrdenEstadoInvalidoException}.
 *
 * Módulo: reposicion.
 */
@Getter
public class TransicionOrdenInvalidaException extends ConflictoDeEstadoException {

    private final UUID ordenId;
    private final EstadoOrden estadoActual;

    public TransicionOrdenInvalidaException(UUID ordenId, EstadoOrden estadoActual, String operacion) {
        super("No se puede " + operacion + " de la orden " + ordenId + " en estado " + estadoActual);
        this.ordenId = ordenId;
        this.estadoActual = estadoActual;
    }
}
