package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Punto de entrada resiliente para las operaciones de inventario.
 *
 * El reintento vive en este bean y delega en {@link InventarioService}, donde
 * cada intento se ejecuta dentro de una transacción independiente. Además del
 * conflicto de versión (D-24), se reintenta la violación de unicidad: dos
 * operaciones simultáneas pueden crear la misma fila por demanda (D-25).
 */
@Service
@RequiredArgsConstructor
public class InventarioRetryableService implements InventarioUseCase {

    private final InventarioService inventarioService;

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad, UUID usuarioId) {
        return inventarioService.cargar(nodoId, varianteId, cantidad, usuarioId);
    }

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public MovimientoInventario aplicarVenta(UUID nodoId, UUID varianteId, int cantidad, boolean forzado,
                                             UUID ventaId) {
        return inventarioService.aplicarVenta(nodoId, varianteId, cantidad, forzado, ventaId);
    }

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public MovimientoInventario aplicarAjuste(UUID nodoId, UUID varianteId, int delta, MotivoAjuste motivo,
                                              String comentario, UUID usuarioId) {
        return inventarioService.aplicarAjuste(nodoId, varianteId, delta, motivo, comentario, usuarioId);
    }

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public MovimientoInventario revertir(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId) {
        return inventarioService.revertir(nodoId, varianteId, cantidad, ventaId);
    }

    @Override
    @Retryable(includes = {OptimisticLockingFailureException.class, DataIntegrityViolationException.class},
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId) {
        inventarioService.trasladar(nodoOrigenId, nodoDestinoId, varianteId, cantidad, traspasoId);
    }
}
