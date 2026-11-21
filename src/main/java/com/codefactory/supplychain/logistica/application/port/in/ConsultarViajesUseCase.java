package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Consulta de viajes con sus paradas (HU-47). */
public interface ConsultarViajesUseCase {

    /** @param estado nulo para todos */
    Page<Viaje> listar(EstadoViaje estado, Pageable pageable);

    Viaje obtener(UUID viajeId);
}
