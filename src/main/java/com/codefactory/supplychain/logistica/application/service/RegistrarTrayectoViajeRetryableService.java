package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.RegistrarTrayectoViajeUseCase;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Punto de entrada de HU-48 con reintento fuera de la transacción (D-24): conflicto de versión
 * del viaje o de una orden.
 *
 * Módulo: logistica.
 */
@Service
@RequiredArgsConstructor
public class RegistrarTrayectoViajeRetryableService implements RegistrarTrayectoViajeUseCase {

    private final RegistrarTrayectoViajeService servicio;

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10,
            multiplier = 2)
    public Viaje registrarSalida(UUID viajeId, Instant fechaSalida) {
        return servicio.registrarSalida(viajeId, fechaSalida);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10,
            multiplier = 2)
    public Viaje registrarLlegada(UUID viajeId, UUID paradaId, Instant fechaLlegada) {
        return servicio.registrarLlegada(viajeId, paradaId, fechaLlegada);
    }
}
