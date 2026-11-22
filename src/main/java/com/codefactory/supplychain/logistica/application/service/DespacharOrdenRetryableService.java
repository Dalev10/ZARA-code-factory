package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.DespacharOrdenUseCase;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Punto de entrada de HU-46 con reintento fuera de la transacción (D-24). Reintenta el
 * conflicto de versión de la orden o del inventario del CD y el choque del
 * {@code UNIQUE (orden_id, numero_ciclo)}: en el reintento, un despacho simultáneo de la misma
 * orden la encuentra DESPACHADA y responde 409.
 *
 * Módulo: logistica.
 */
@Service
@RequiredArgsConstructor
public class DespacharOrdenRetryableService implements DespacharOrdenUseCase {

    private final DespacharOrdenService despacharOrdenService;

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public CicloDespacho despachar(UUID ordenId) {
        return despacharOrdenService.despachar(ordenId);
    }
}
