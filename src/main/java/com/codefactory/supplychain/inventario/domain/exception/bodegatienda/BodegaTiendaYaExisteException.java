package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

/**
 * Excepción lanzada cuando se intenta registrar una {@code BodegaTienda}
 * para una tienda que ya cuenta con una bodega asignada.
 */
public class BodegaTiendaYaExisteException extends RuntimeException {

    public BodegaTiendaYaExisteException(String mensaje) {
        super(mensaje);
    }

    public BodegaTiendaYaExisteException(Long tiendaId) {
        super(String.format("Ya existe una bodega de tienda registrada para la tienda con ID: %d", tiendaId));
    }

    public static BodegaTiendaYaExisteException porTiendaId(Long tiendaId) {
        return new BodegaTiendaYaExisteException(
                String.format("Ya existe una bodega de tienda registrada para la tienda con ID: %d", tiendaId));
    }
}
