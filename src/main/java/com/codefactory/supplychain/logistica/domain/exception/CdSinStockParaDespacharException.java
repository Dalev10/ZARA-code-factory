package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

import java.util.UUID;

/** El CD de la orden no tiene nada a la mano: no se crea ciclo ni cambia la orden (HU-46, 409). */
public class CdSinStockParaDespacharException extends ConflictoDeEstadoException {

    public CdSinStockParaDespacharException(UUID ordenId) {
        super("El CD no tiene unidades a la mano para despachar la orden " + ordenId);
    }
}
