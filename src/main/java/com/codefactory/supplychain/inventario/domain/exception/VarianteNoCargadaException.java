package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Indica que todavía no existe una fila de Inventario para el nodo y variante.
 *
 * TODO: debe responder 409 (compuerta de carga, plan HU-27), incluso en venta
 * forzada. Hoy GlobalExceptionHandler de shared/ traduce toda
 * ReglaDeNegocioException a 400. Separar conflictos de validaciones en shared/
 * y ajustar las pruebas afectadas.
 */
public class VarianteNoCargadaException extends ReglaDeNegocioException {

    public VarianteNoCargadaException(UUID nodoId, UUID varianteId) {
        super("La variante " + varianteId + " no está cargada en el nodo " + nodoId);
    }
}
