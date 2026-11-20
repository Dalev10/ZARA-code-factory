package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.CdsPorPrioridadPort;
import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort.Reserva;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Fallback de CD.
 * <p>
 * Recorre {@code cdsPorPrioridad(tienda)} en orden y llama {@code reservar} en cada CD.
 * {@code SIN_DISPONIBLE} → intento SIN_STOCK y sigue; {@code ASIGNADO} → intento
 * ASIGNADO, orden APROBADA con ese CD y termina. Agotada la lista → GENERADA sin CD.
 * <p>
 * Sin {@code @Retryable}: corre en la transacción del llamador. Si otra orden reservó
 * el mismo stock a la vez, el conflicto de {@code @Version} de la fila del CD sale al
 * confirmar y el caso de uso de entrada reintenta la transacción completa; en el
 * reintento ese CD ya no cubre y la orden pasa al siguiente.
 */
@Service
@Transactional
public class CrearOrdenService implements CrearOrdenUseCase {

    private final CdsPorPrioridadPort cdsPorPrioridad;
    private final ReservarStockCdPort reservarStock;
    private final OrdenReabastecimientoRepository ordenes;
    private final Clock clock;

    public CrearOrdenService(CdsPorPrioridadPort cdsPorPrioridad,
                             ReservarStockCdPort reservarStock,
                             OrdenReabastecimientoRepository ordenes,
                             ObjectProvider<Clock> clock) {
        this.cdsPorPrioridad = cdsPorPrioridad;
        this.reservarStock = reservarStock;
        this.ordenes = ordenes;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Override
    public OrdenReabastecimiento crear(UUID recomendacionId, UUID tiendaId, UUID varianteId, int cantidad) {
        var orden = OrdenReabastecimiento.iniciar(recomendacionId, tiendaId, varianteId, cantidad, clock.instant());
        List<UUID> cds = cdsPorPrioridad.cdsPorPrioridad(tiendaId);
        for (int i = 0; i < cds.size(); i++) {
            UUID cdId = cds.get(i);
            int posicion = i + 1;
            Reserva reserva = reservarStock.reservar(cdId, tiendaId, varianteId, cantidad, orden.getId());
            if (reserva.asignada()) {
                orden.asignar(cdId, posicion, clock.instant());
                return ordenes.guardar(orden);
            }
            orden.registrarSinStock(cdId, posicion, reserva.disponible(), clock.instant());
        }
        orden.quedarSinCd();
        return ordenes.guardar(orden);
    }
}
