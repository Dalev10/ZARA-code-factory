package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.UUID;

/**
 * Indica que ya existe una carga inicial para el nodo y la variante (D-28).
 */
public class InventarioYaCargadoException extends RecursoDuplicadoException {

    public InventarioYaCargadoException(UUID nodoId, UUID varianteId) {
        super("Ya existe inventario cargado para la variante " + varianteId
                + " en el nodo " + nodoId);
    }
}
