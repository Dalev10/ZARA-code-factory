package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.ProhibidoException;

import java.util.UUID;

/** El usuario no tiene asignada la tienda sobre la que intenta operar. HTTP 403. */
public class TiendaNoPermitidaException extends ProhibidoException {

    public TiendaNoPermitidaException(UUID tiendaId) {
        super("No tiene acceso a la tienda " + tiendaId);
    }
}
