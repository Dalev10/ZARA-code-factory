package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.CerrarOrdenUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Punto de entrada de HU-53 con reintento fuera de la transacción (D-24): conflicto de versión de
 * la orden o del inventario. Si otra operación cambió el estado, el reintento responde 409.
 *
 * Módulo: reposicion.
 */
@Service
@RequiredArgsConstructor
public class CerrarOrdenRetryableService implements CerrarOrdenUseCase {

    private final CerrarOrdenService servicio;

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10,
            multiplier = 2)
    public OrdenReabastecimiento cerrarConFaltante(UsuarioActual usuario, UUID ordenId, String motivo) {
        return servicio.cerrarConFaltante(usuario, ordenId, motivo);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10,
            multiplier = 2)
    public OrdenReabastecimiento cancelar(UsuarioActual usuario, UUID ordenId, String motivo) {
        return servicio.cancelar(usuario, ordenId, motivo);
    }
}
