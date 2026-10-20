package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Un ajuste no puede dejar {@code aLaMano} por debajo de cero, a diferencia
 * de la venta forzada (HU-38). HTTP 400.
 */
public class AjusteDejaStockNegativoException extends ReglaDeNegocioException {

    public AjusteDejaStockNegativoException(int aLaMano, int delta) {
        super("El ajuste dejaría el stock en negativo: a la mano=" + aLaMano + ", delta=" + delta);
    }
}
