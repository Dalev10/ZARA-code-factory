package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;

import java.util.UUID;

/**
 * Casos de uso internos para modificar un Inventario ya cargado.
 */
public interface InventarioUseCase {

    MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad, UUID usuarioId);

    MovimientoInventario reponer(UUID nodoId, UUID varianteId, int cantidad);

    MovimientoInventario aplicarVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            boolean forzado);

    MovimientoInventario aplicarAjuste(
            UUID nodoId,
            UUID varianteId,
            int delta,
            String motivo);

    MovimientoInventario revertir(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            String motivo);
}
