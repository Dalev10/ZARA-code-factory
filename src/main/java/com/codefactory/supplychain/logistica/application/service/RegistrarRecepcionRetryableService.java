package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Punto de entrada de HU-49 con reintento fuera de la transacción de
 * {@link RegistrarRecepcionService} (D-24). Se reintenta:
 * <ul>
 *   <li>el conflicto de versión del inventario de la Bodega_Tienda o de la orden, y</li>
 *   <li>la violación de unicidad: otra transacción registró la misma parada primero. En el
 *       reintento se encuentra esa recepción y se devuelve con 200 (D-91).</li>
 * </ul>
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Service
@RequiredArgsConstructor
public class RegistrarRecepcionRetryableService implements RegistrarRecepcionUseCase {

    private final RegistrarRecepcionService registrarRecepcionService;

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public ResultadoRecepcion registrar(Comando comando) {
        return registrarRecepcionService.registrar(comando);
    }
}
