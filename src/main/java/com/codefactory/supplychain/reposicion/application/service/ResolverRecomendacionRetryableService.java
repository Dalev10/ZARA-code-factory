package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.ResolverRecomendacionUseCase;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reintenta por {@code @Version} <b>fuera</b> de la transacción
 * Cada intento abre una transacción nueva en {@link ResolverRecomendacionService}, vuelve a
 * leer la recomendación y, si otro ya la resolvió, responde 409 sin crear otra orden.
 *
 * Módulo: reposicion.
 */
@Service
public class ResolverRecomendacionRetryableService implements ResolverRecomendacionUseCase {

    private final ResolverRecomendacionService service;

    public ResolverRecomendacionRetryableService(ResolverRecomendacionService service) {
        this.service = service;
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class,
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public Aprobacion aprobar(UsuarioActual usuario, UUID recomendacionId, Integer cantidadAprobada) {
        return service.aprobar(usuario, recomendacionId, cantidadAprobada);
    }

    @Override
    @Retryable(includes = OptimisticLockingFailureException.class,
            maxRetries = 3, delay = 10, jitter = 10, multiplier = 2)
    public Recomendacion rechazar(UsuarioActual usuario, UUID recomendacionId) {
        return service.rechazar(usuario, recomendacionId);
    }
}
