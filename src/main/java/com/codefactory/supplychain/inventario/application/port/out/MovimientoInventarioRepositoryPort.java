package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;

import java.util.UUID;

/**
 * Puerto de persistencia para los movimientos del kardex.
 */
public interface MovimientoInventarioRepositoryPort {

    MovimientoInventario guardar(
            UUID nodoId,
            UUID varianteId,
            MovimientoInventario movimiento);
}
