package com.codefactory.supplychain.logistica.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.logistica.application.port.out.InventarioRecepcionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code inventario.api}: la recepción suma lo recibido en la Bodega_Tienda.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Component
@RequiredArgsConstructor
class InventarioRecepcionAdapter implements InventarioRecepcionPort {

    private final MovimientosAbastecimiento movimientos;

    @Override
    public void sumarRecibido(UUID tiendaId, UUID varianteId, int cantidad, UUID recepcionId) {
        movimientos.registrarRecepcion(tiendaId, varianteId, cantidad, recepcionId);
    }
}
