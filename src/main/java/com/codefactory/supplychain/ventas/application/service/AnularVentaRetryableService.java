package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.AnularVentaUseCase;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Reintenta fuera de la transacción cuando varias anulaciones compiten por las mismas existencias. */
@Service
@RequiredArgsConstructor
public class AnularVentaRetryableService implements AnularVentaUseCase {

    private final AnularVentaService servicio;

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public Venta anular(UsuarioActual usuario, UUID ventaId, String motivo) {
        return servicio.anular(usuario, ventaId, motivo);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public List<Venta> anularTransaccion(UsuarioActual usuario, UUID transaccionId, String motivo) {
        return servicio.anularTransaccion(usuario, transaccionId, motivo);
    }
}
