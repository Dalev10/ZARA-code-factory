package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Punto de entrada de HU-47 con reintento fuera de la transacción (D-24): si otro viaje cargó el
 * mismo ciclo a la vez, el {@code UNIQUE} hace fallar uno y su reintento responde 409.
 *
 * Módulo: logistica.
 */
@Service
@RequiredArgsConstructor
public class ArmarViajeRetryableService implements ArmarViajeUseCase {

    private final ArmarViajeService armarViajeService;

    @Override
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 3, delay = 10, jitter = 10,
            multiplier = 2)
    public Viaje armar(Comando comando) {
        return armarViajeService.armar(comando);
    }
}
