package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.api.ResultadoReserva;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia inventario.api: traduce {@link ResultadoReserva} a la reserva de reposicion.
 *
 * Módulo: reposicion.
 */
@Component
public class ReservarStockCdAdapter implements ReservarStockCdPort {

    private final ReservasInventario reservasInventario;

    public ReservarStockCdAdapter(ReservasInventario reservasInventario) {
        this.reservasInventario = reservasInventario;
    }

    @Override
    public Reserva reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID ordenId) {
        ResultadoReserva resultado = reservasInventario.reservar(cdId, tiendaId, varianteId, cantidad, ordenId);
        return new Reserva(resultado.asignada(), resultado.disponibleActual());
    }
}
