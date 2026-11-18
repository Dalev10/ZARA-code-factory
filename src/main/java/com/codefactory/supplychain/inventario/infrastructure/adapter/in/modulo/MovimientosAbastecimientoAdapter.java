package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación de {@link MovimientosAbastecimiento}.
 *
 * TODO HD-01 paso 3: delegar en el servicio de abastecimiento. Mientras tanto
 * el bean existe para que los consumidores puedan inyectar el contrato, pero
 * cualquier llamada falla de forma explícita.
 *
 * Módulo: inventario.
 */
@Component
class MovimientosAbastecimientoAdapter implements MovimientosAbastecimiento {

    @Override
    public void registrarDespacho(
            UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar, UUID referenciaId) {
        throw new UnsupportedOperationException("registrarDespacho se implementa en HD-01 paso 3");
    }

    @Override
    public void registrarRecepcion(UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        throw new UnsupportedOperationException("registrarRecepcion se implementa en HD-01 paso 3");
    }
}
