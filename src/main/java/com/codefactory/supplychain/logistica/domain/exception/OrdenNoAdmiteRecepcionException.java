package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/**
 * La orden de reabastecimiento de la parada no está en un estado que admita la recepción
 * (debe estar EN_TRANSITO). Traduce el rechazo de reposición sin exponer sus tipos (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class OrdenNoAdmiteRecepcionException extends ConflictoDeEstadoException {

    public OrdenNoAdmiteRecepcionException(String mensaje) {
        super(mensaje);
    }
}
