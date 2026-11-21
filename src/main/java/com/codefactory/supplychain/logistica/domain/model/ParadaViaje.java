package com.codefactory.supplychain.logistica.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Una parada del viaje: lleva un ciclo de despacho a una tienda (HU-47). Solo cambia a través
 * de su {@link Viaje}.
 *
 * Módulo: logistica.
 */
@Getter
public final class ParadaViaje {

    private final UUID id;
    private final UUID cicloDespachoId;
    private final UUID tiendaId;
    private final int ordenParada;
    private EstadoParada estado;
    private Instant fechaSalidaCd;
    private Instant fechaLlegada;

    private ParadaViaje(UUID id, UUID cicloDespachoId, UUID tiendaId, int ordenParada, EstadoParada estado,
                        Instant fechaSalidaCd, Instant fechaLlegada) {
        this.id = id;
        this.cicloDespachoId = cicloDespachoId;
        this.tiendaId = tiendaId;
        this.ordenParada = ordenParada;
        this.estado = estado;
        this.fechaSalidaCd = fechaSalidaCd;
        this.fechaLlegada = fechaLlegada;
    }

    void salir(Instant fecha) {
        estado = EstadoParada.EN_TRANSITO;
        fechaSalidaCd = fecha;
    }

    void llegar(Instant fecha) {
        estado = EstadoParada.ENTREGADO;
        fechaLlegada = fecha;
    }

    static ParadaViaje nueva(UUID cicloDespachoId, UUID tiendaId, int ordenParada) {
        return new ParadaViaje(UUID.randomUUID(), cicloDespachoId, tiendaId, ordenParada, EstadoParada.PENDIENTE,
                null, null);
    }

    public static ParadaViaje reconstruir(UUID id, UUID cicloDespachoId, UUID tiendaId, int ordenParada,
                                          EstadoParada estado, Instant fechaSalidaCd, Instant fechaLlegada) {
        return new ParadaViaje(id, cicloDespachoId, tiendaId, ordenParada, estado, fechaSalidaCd, fechaLlegada);
    }
}
