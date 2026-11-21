package com.codefactory.supplychain.logistica.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Los datos de una recepción violan una invariante: cantidad recibida o despachada negativa,
 * o falta la parada, el ciclo, el usuario que la registra o la fecha.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public class RecepcionInvalidaException extends ReglaDeNegocioException {

    public RecepcionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
