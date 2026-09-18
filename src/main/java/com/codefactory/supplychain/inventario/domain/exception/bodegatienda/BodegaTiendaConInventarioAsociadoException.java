package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

/**
 * Excepción reservada para impedir la eliminación de una bodega con inventario
 * asociado cuando HU-10 defina la validación completa.
 */
public class BodegaTiendaConInventarioAsociadoException extends RuntimeException {

    public BodegaTiendaConInventarioAsociadoException(Long nodoId) {
        super(String.format("La bodega de tienda con ID %d tiene inventario asociado", nodoId));
    }
}
