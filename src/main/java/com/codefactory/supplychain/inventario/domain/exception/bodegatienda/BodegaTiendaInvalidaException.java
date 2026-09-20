package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

/**
 * Excepción lanzada cuando los datos proporcionados para la creación
 * o reconstrucción de una {@code BodegaTienda} violan las invariantes del
 * dominio.
 */
public class BodegaTiendaInvalidaException extends IllegalArgumentException {

    public BodegaTiendaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public BodegaTiendaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
