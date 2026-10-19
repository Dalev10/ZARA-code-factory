package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;

/**
 * Corrige una fila de inventario existente con motivo cerrado (D-29).
 */
public interface AjustarInventarioUseCase {

    MovimientoInventario ajustar(AjusteInventarioComando comando);
}
