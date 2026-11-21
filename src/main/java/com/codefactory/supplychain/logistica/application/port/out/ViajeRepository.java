package com.codefactory.supplychain.logistica.application.port.out;

import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Persistencia del viaje con sus paradas. El viaje lleva {@code @Version}. */
public interface ViajeRepository {

    Viaje guardar(Viaje viaje);

    Optional<Viaje> buscarPorId(UUID viajeId);

    /**
     * Como {@link #buscarPorId} pero sube la versión del viaje al confirmar aunque solo cambie una
     * parada: dos llegadas simultáneas chocan y una reintenta (HU-48).
     */
    Optional<Viaje> buscarParaActualizar(UUID viajeId);

    Page<Viaje> listar(EstadoViaje estado, Pageable pageable);

    boolean cicloEnViaje(UUID cicloDespachoId);
}
