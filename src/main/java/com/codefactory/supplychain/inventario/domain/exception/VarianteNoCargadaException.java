package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Indica que todavía no existe una fila de Inventario para el nodo y variante.
 */
public class VarianteNoCargadaException extends ReglaDeNegocioException {

    public VarianteNoCargadaException(UUID nodoId, UUID varianteId) {
        super("La variante " + varianteId + " no está cargada en el nodo " + nodoId);
    }
}
