package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/** La orden no está APROBADA ni REABIERTA, así que no admite un despacho (HU-46, 409). */
public class OrdenNoDespachableException extends ConflictoDeEstadoException {

    public OrdenNoDespachableException(String mensaje) {
        super(mensaje);
    }
}
