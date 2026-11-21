package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.application.port.out.OrdenViajePort;
import com.codefactory.supplychain.logistica.application.port.out.ViajeRepository;
import com.codefactory.supplychain.logistica.domain.exception.CicloDespachoNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.exception.ViajeInvalidoException;
import com.codefactory.supplychain.logistica.domain.exception.ViajeNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import com.codefactory.supplychain.logistica.domain.model.ParadaViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Lógica transaccional de HU-48. El viaje se carga subiendo su versión, así que dos registros
 * simultáneos sobre el mismo viaje chocan y uno reintenta en
 * {@link RegistrarTrayectoViajeRetryableService}: ninguna llegada deja el viaje sin cerrar.
 * <p>
 * La salida pasa a EN_TRANSITO las órdenes de todas las paradas en la misma transacción.
 *
 * Módulo: logistica.
 */
@Service
public class RegistrarTrayectoViajeService {

    /** Tolera un reloj de cliente algo adelantado; más allá, la hora se rechaza como futura (400). */
    static final Duration MARGEN_RELOJ_CLIENTE = Duration.ofMinutes(2);

    private final ViajeRepository viajes;
    private final CicloDespachoRepository ciclos;
    private final OrdenViajePort ordenes;
    private final Clock reloj;

    public RegistrarTrayectoViajeService(ViajeRepository viajes, CicloDespachoRepository ciclos,
                                         OrdenViajePort ordenes, Clock reloj) {
        this.viajes = viajes;
        this.ciclos = ciclos;
        this.ordenes = ordenes;
        this.reloj = reloj;
    }

    @Transactional
    public Viaje registrarSalida(UUID viajeId, Instant fechaSalida) {
        Instant fecha = horaDelRegistro(fechaSalida, "salida");
        Viaje viaje = cargar(viajeId);
        List<CicloDespacho> despachos = new ArrayList<>();
        for (ParadaViaje parada : viaje.getParadas()) {
            despachos.add(ciclos.buscarPorId(parada.getCicloDespachoId())
                    .orElseThrow(() -> new CicloDespachoNoEncontradoException(parada.getCicloDespachoId())));
        }
        Instant ultimoDespacho = despachos.stream().map(CicloDespacho::getFechaDespacho)
                .max(Comparator.naturalOrder()).orElse(Instant.MIN);
        viaje.registrarSalida(fecha, ultimoDespacho);
        despachos.forEach(ciclo -> ordenes.marcarEnTransito(ciclo.getOrdenId()));
        return viajes.guardar(viaje);
    }

    @Transactional
    public Viaje registrarLlegada(UUID viajeId, UUID paradaId, Instant fechaLlegada) {
        Instant fecha = horaDelRegistro(fechaLlegada, "llegada");
        Viaje viaje = cargar(viajeId);
        viaje.registrarLlegada(paradaId, fecha);
        return viajes.guardar(viaje);
    }

    private Viaje cargar(UUID viajeId) {
        return viajes.buscarParaActualizar(viajeId).orElseThrow(() -> new ViajeNoEncontradoException(viajeId));
    }

    private Instant horaDelRegistro(Instant enviada, String que) {
        Instant ahora = Instant.now(reloj);
        if (enviada == null) {
            return ahora;
        }
        if (enviada.isAfter(ahora.plus(MARGEN_RELOJ_CLIENTE))) {
            throw new ViajeInvalidoException("La hora de " + que + " no puede ser posterior a la actual");
        }
        return enviada;
    }
}
