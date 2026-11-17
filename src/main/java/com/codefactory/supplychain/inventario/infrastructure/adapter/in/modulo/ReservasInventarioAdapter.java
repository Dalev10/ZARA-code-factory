package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.api.ResultadoReserva;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación de {@link ReservasInventario}.
 *
 * TODO HD-01 paso 2 (V29): delegar en el servicio de abastecimiento. Mientras
 * tanto el bean existe para que los consumidores puedan inyectar el contrato,
 * pero cualquier llamada falla de forma explícita.
 *
 * Módulo: inventario.
 */
@Component
class ReservasInventarioAdapter implements ReservasInventario {

    @Override
    public ResultadoReserva reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        throw new UnsupportedOperationException("reservar se implementa en HD-01 paso 2");
    }

    @Override
    public void liberar(
            UUID cdId,
            UUID tiendaId,
            UUID varianteId,
            int reservaALiberar,
            int entranteALiberar,
            UUID referenciaId) {
        throw new UnsupportedOperationException("liberar se implementa en HD-01 paso 2");
    }
}
