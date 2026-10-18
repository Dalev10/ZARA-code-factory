package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Punto de entrada de HU-31 con reintento por conflicto de versión del inventario (D-24).
 * El conflicto aparece al confirmar la transacción de {@link VentaService}, por eso el
 * reintento envuelve a esa transacción y no puede vivir dentro de ella.
 */
@Service
@RequiredArgsConstructor
public class VentaRetryableService implements RegistrarVentaUseCase {

    private final VentaService ventaService;

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10)
    public ResultadoVenta registrar(Comando comando) {
        return ventaService.registrar(comando);
    }
}
