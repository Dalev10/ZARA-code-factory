package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.ViajeInvalidoException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un viaje que parte de un CD con varias paradas, una por ciclo de despacho (HU-47).
 * Las reglas que necesitan otros datos (el CD del ciclo, si ya está en otro viaje) las valida
 * el caso de uso; aquí solo las de forma del viaje.
 *
 * Módulo: logistica.
 */
@Getter
public final class Viaje {

    private final UUID id;
    private final UUID cdId;
    private final String vehiculo;
    private final Instant fechaCreacion;
    private final Long version;
    private final List<ParadaViaje> paradas;
    private EstadoViaje estado;
    private Instant fechaSalida;
    private Instant fechaCierre;

    private Viaje(UUID id, UUID cdId, String vehiculo, Instant fechaCreacion, Long version, List<ParadaViaje> paradas,
                  EstadoViaje estado, Instant fechaSalida, Instant fechaCierre) {
        this.id = id;
        this.cdId = cdId;
        this.vehiculo = vehiculo;
        this.fechaCreacion = fechaCreacion;
        this.version = version;
        this.paradas = paradas;
        this.estado = estado;
        this.fechaSalida = fechaSalida;
        this.fechaCierre = fechaCierre;
    }

    /**
     * @param ordenParada nulo para usar la posición en la lista (1, 2, ...)
     */
    public record NuevaParada(UUID cicloDespachoId, UUID tiendaId, Integer ordenParada) {
    }

    /**
     * Arma el viaje en ARMADO con sus paradas ordenadas.
     *
     * @throws ViajeInvalidoException sin paradas, sin vehículo, con un ciclo repetido o con un orden
     *                                de parada repetido o no positivo
     */
    public static Viaje armar(UUID cdId, String vehiculo, List<NuevaParada> nuevas, Instant ahora) {
        if (vehiculo == null || vehiculo.isBlank()) {
            throw new ViajeInvalidoException("El vehículo es obligatorio");
        }
        if (nuevas == null || nuevas.isEmpty()) {
            throw new ViajeInvalidoException("Un viaje necesita al menos una parada");
        }
        Set<UUID> ciclos = new HashSet<>();
        Set<Integer> ordenes = new HashSet<>();
        List<ParadaViaje> paradas = new ArrayList<>();
        for (int i = 0; i < nuevas.size(); i++) {
            NuevaParada nueva = nuevas.get(i);
            int orden = nueva.ordenParada() != null ? nueva.ordenParada() : i + 1;
            if (orden <= 0) {
                throw new ViajeInvalidoException("El orden de parada debe ser mayor que cero");
            }
            if (!ciclos.add(nueva.cicloDespachoId())) {
                throw new ViajeInvalidoException("El ciclo de despacho " + nueva.cicloDespachoId()
                        + " está repetido en el viaje");
            }
            if (!ordenes.add(orden)) {
                throw new ViajeInvalidoException("El orden de parada " + orden + " está repetido");
            }
            paradas.add(ParadaViaje.nueva(nueva.cicloDespachoId(), nueva.tiendaId(), orden));
        }
        paradas.sort(Comparator.comparingInt(ParadaViaje::getOrdenParada));
        return new Viaje(UUID.randomUUID(), cdId, vehiculo.strip(), ahora, null, paradas, EstadoViaje.ARMADO,
                null, null);
    }

    public static Viaje reconstruir(UUID id, UUID cdId, String vehiculo, Instant fechaCreacion, Long version,
                                    List<ParadaViaje> paradas, EstadoViaje estado, Instant fechaSalida,
                                    Instant fechaCierre) {
        List<ParadaViaje> ordenadas = new ArrayList<>(paradas);
        ordenadas.sort(Comparator.comparingInt(ParadaViaje::getOrdenParada));
        return new Viaje(id, cdId, vehiculo, fechaCreacion, version, ordenadas, estado, fechaSalida, fechaCierre);
    }

    public List<ParadaViaje> getParadas() {
        return List.copyOf(paradas);
    }
}
