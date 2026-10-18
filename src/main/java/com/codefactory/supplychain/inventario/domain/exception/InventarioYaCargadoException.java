package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Indica que ya existe una carga inicial para el nodo y la variante.
 *
 * TODO: debe responder 409 (fila ya existente, AGENTS.md §6 y D-28). Hoy
 * GlobalExceptionHandler de shared/ traduce toda ReglaDeNegocioException a 400.
 * Separar conflictos de validaciones en shared/ y ajustar las pruebas afectadas.
 */
public class InventarioYaCargadoException extends ReglaDeNegocioException {

    public InventarioYaCargadoException(UUID nodoId, UUID varianteId) {
        super("Ya existe inventario cargado para la variante " + varianteId
                + " en el nodo " + nodoId);
    }
}
