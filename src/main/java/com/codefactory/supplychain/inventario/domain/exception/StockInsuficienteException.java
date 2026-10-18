package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Indica que una venta normal excede las existencias disponibles.
 *
 * TODO(shared): debe responder 409 (conflicto de negocio, D-35). Hoy
 * GlobalExceptionHandler de shared/ traduce toda ReglaDeNegocioException a 400.
 * Separar conflictos de validaciones en shared/ y ajustar las pruebas afectadas.
 */
public class StockInsuficienteException extends ReglaDeNegocioException {

    public StockInsuficienteException(int disponible, int solicitado) {
        super("Stock insuficiente: disponible=" + disponible + ", solicitado=" + solicitado);
    }
}
