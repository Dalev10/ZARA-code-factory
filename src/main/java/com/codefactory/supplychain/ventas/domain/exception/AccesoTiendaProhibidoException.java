package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ProhibidoException;

/** El usuario no puede operar en la tienda indicada en el cuerpo (HTTP 403). */
public class AccesoTiendaProhibidoException extends ProhibidoException {
    public AccesoTiendaProhibidoException(java.util.UUID tiendaId) {
        super("No tenés acceso a la tienda " + tiendaId);
    }
}
