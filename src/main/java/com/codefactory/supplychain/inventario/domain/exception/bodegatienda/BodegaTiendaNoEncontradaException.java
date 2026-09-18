package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

/**
 * Excepción lanzada cuando no se encuentra una {@code BodegaTienda}
 * buscada por su identificador único o por el identificador de la tienda
 * asociada.
 */
public class BodegaTiendaNoEncontradaException extends RuntimeException {

    private BodegaTiendaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static BodegaTiendaNoEncontradaException porId(Long id) {
        return new BodegaTiendaNoEncontradaException(
                String.format("No se encontró la bodega de tienda con ID: %d", id));
    }

    public static BodegaTiendaNoEncontradaException porTiendaId(Long tiendaId) {
        return new BodegaTiendaNoEncontradaException(
                String.format("No se encontró bodega de tienda asociada a la tienda con ID: %d", tiendaId));
    }
}
