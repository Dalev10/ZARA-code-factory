package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Punto de entrada resiliente para las operaciones de inventario.
 *
 * El reintento vive en este bean y delega en {@link InventarioService}, donde
 * cada intento se ejecuta dentro de una transacción independiente.
 */
@Service
@RequiredArgsConstructor
public class InventarioRetryableService implements InventarioUseCase {

    private final InventarioService inventarioService;

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10)
    public MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad) {
        return inventarioService.cargar(nodoId, varianteId, cantidad);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10)
    public MovimientoInventario reponer(UUID nodoId, UUID varianteId, int cantidad) {
        return inventarioService.reponer(nodoId, varianteId, cantidad);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10)
    public MovimientoInventario aplicarVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            boolean forzado) {
        return inventarioService.aplicarVenta(nodoId, varianteId, cantidad, forzado);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10)
    public MovimientoInventario aplicarAjuste(
            UUID nodoId,
            UUID varianteId,
            int delta,
            String motivo) {
        return inventarioService.aplicarAjuste(nodoId, varianteId, delta, motivo);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10)
    public MovimientoInventario revertir(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            String motivo) {
        return inventarioService.revertir(nodoId, varianteId, cantidad, motivo);
    }
}
