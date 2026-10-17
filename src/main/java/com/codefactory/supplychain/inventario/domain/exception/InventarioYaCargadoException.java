package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Indica que ya existe una carga inicial para el nodo y la variante.
 */
public class InventarioYaCargadoException extends ReglaDeNegocioException {

    public InventarioYaCargadoException(UUID nodoId, UUID varianteId) {
        super("Ya existe inventario cargado para la variante " + varianteId
                + " en el nodo " + nodoId);
    }
}
