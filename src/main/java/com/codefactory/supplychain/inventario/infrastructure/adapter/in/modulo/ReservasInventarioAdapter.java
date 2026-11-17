package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.api.ResultadoReserva;
import com.codefactory.supplychain.inventario.application.port.in.ReservarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.in.ResultadoReservaCd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementa {@link ReservasInventario} (API pública) sobre
 * {@link ReservarInventarioUseCase}. Solo traduce: las reglas viven en el
 * agregado {@code Inventario}.
 *
 * Módulo: inventario.
 */
@Component
@RequiredArgsConstructor
class ReservasInventarioAdapter implements ReservasInventario {

    private final ReservarInventarioUseCase reservarInventario;

    @Override
    public ResultadoReserva reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        ResultadoReservaCd resultado = reservarInventario.reservar(cdId, tiendaId, varianteId, cantidad, referenciaId);
        return resultado.asignada()
                ? ResultadoReserva.asignado(resultado.disponibleActual())
                : ResultadoReserva.sinDisponible(resultado.disponibleActual());
    }

    @Override
    public void liberar(
            UUID cdId,
            UUID tiendaId,
            UUID varianteId,
            int reservaALiberar,
            int entranteALiberar,
            UUID referenciaId) {
        reservarInventario.liberar(cdId, tiendaId, varianteId, reservaALiberar, entranteALiberar, referenciaId);
    }
}
