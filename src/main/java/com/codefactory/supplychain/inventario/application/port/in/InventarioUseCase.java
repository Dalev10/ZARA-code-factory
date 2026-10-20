package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;

import java.util.UUID;

/**
 * Casos de uso internos que modifican existencias. Cada operación registra su
 * movimiento en el kardex y publica {@code ExistenciasActualizadas}.
 */
public interface InventarioUseCase {

    MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad, UUID usuarioId);

    MovimientoInventario aplicarVenta(UUID nodoId, UUID varianteId, int cantidad, boolean forzado, UUID ventaId);

    MovimientoInventario aplicarAjuste(UUID nodoId, UUID varianteId, int delta, MotivoAjuste motivo,
                                       String comentario, UUID usuarioId);

    MovimientoInventario revertir(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId);

    void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId);
}
