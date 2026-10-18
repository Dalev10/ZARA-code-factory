package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Se intenta eliminar un CD que tiene prioridades asignadas a una o más tiendas.
 *
 * Módulo: inventario (HU-42).
 */
public class CdEnUsoPorPrioridadException extends ReglaDeNegocioException {

    public CdEnUsoPorPrioridadException(UUID cdId) {
        super("El CD " + cdId + " tiene prioridades asignadas y no puede eliminarse. "
                + "Elimine primero las prioridades con PUT /tiendas/{id}/prioridad-cd");
    }
}
