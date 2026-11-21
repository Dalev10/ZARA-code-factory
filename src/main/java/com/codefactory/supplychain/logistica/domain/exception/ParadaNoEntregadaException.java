package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

/**
 * Se intentó registrar la recepción de una parada que todavía no está ENTREGADO (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class ParadaNoEntregadaException extends ConflictoDeEstadoException {

    public ParadaNoEntregadaException(UUID paradaId) {
        super("La parada " + paradaId + " todavía no se ha entregado");
    }
}
