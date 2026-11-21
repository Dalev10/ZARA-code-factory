package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.logistica.domain.model.EstadoParada;
import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.domain.model.ParadaViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Un viaje con sus paradas, en orden de parada (HU-47, HU-48). */
public record ViajeResponse(
        UUID id,
        UUID cdId,
        String vehiculo,
        EstadoViaje estado,
        Instant fechaCreacion,
        Instant fechaSalida,
        Instant fechaCierre,
        List<Parada> paradas) {

    public record Parada(UUID id, UUID cicloDespachoId, UUID tiendaId, int ordenParada, EstadoParada estado,
                         Instant fechaSalidaCd, Instant fechaLlegada) {

        static Parada desde(ParadaViaje p) {
            return new Parada(p.getId(), p.getCicloDespachoId(), p.getTiendaId(), p.getOrdenParada(), p.getEstado(),
                    p.getFechaSalidaCd(), p.getFechaLlegada());
        }
    }

    public static ViajeResponse desde(Viaje viaje) {
        return new ViajeResponse(viaje.getId(), viaje.getCdId(), viaje.getVehiculo(), viaje.getEstado(),
                viaje.getFechaCreacion(), viaje.getFechaSalida(), viaje.getFechaCierre(),
                viaje.getParadas().stream().map(Parada::desde).toList());
    }
}
