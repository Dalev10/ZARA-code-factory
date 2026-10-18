package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoNoEncontradoException;

public class TiendaVentaNoEncontradaException extends RecursoNoEncontradoException {
    public TiendaVentaNoEncontradaException() { super("La tienda no existe o no tiene Almacén"); }
}
