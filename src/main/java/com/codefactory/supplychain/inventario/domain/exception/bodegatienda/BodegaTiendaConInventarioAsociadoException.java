package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

/**
 * Excepción lanzada cuando una bodega de tienda tiene inventario asociado.
 */
public class BodegaTiendaConInventarioAsociadoException extends RuntimeException {

    private BodegaTiendaConInventarioAsociadoException(String mensaje) {
        super(mensaje);
    }

    public static BodegaTiendaConInventarioAsociadoException porId(Long id) {
        return new BodegaTiendaConInventarioAsociadoException(
                String.format("La bodega de tienda con ID %d tiene inventario asociado", id));
    }
}
