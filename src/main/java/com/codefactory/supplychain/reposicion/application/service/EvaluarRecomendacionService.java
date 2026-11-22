package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluarRecomendacionUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.CdsPorPrioridadPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.RecomendacionRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort.NivelAplicable;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Evalúa si un SKU de una tienda necesita reposición desde el CD.
 * <p>
 * Orden de las comprobaciones: nivel configurado → sin BORRADOR previo → sin
 * orden GENERADA sin CD → regla del dominio. El pronosticado se
 * lee de la Bodega en el momento de evaluar, así que una orden con CD ya sumada como
 * {@code entrante} queda incluida y no se recomienda de nuevo lo pedido.
 * <p>
 * El nivel lo resuelve {@link ResolverNivelPort} (HD-04).
 */
@Service
@Transactional
public class EvaluarRecomendacionService implements EvaluarRecomendacionUseCase {

    private final ResolverNivelPort resolverNivel;
    private final ConsultarExistenciasPort consultarExistencias;
    private final CdsPorPrioridadPort cdsPorPrioridad;
    private final RecomendacionRepository recomendaciones;
    private final Clock clock;

    public EvaluarRecomendacionService(ResolverNivelPort resolverNivel,
                                       ConsultarExistenciasPort consultarExistencias,
                                       CdsPorPrioridadPort cdsPorPrioridad,
                                       RecomendacionRepository recomendaciones,
                                       ObjectProvider<Clock> clock) {
        this.resolverNivel = resolverNivel;
        this.consultarExistencias = consultarExistencias;
        this.cdsPorPrioridad = cdsPorPrioridad;
        this.recomendaciones = recomendaciones;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Override
    public Optional<Recomendacion> evaluar(UUID tiendaId, UUID varianteId) {
        Optional<NivelAplicable> nivel = resolverNivel.resolver(tiendaId, varianteId);
        if (nivel.isEmpty()
                || recomendaciones.existeBorrador(tiendaId, varianteId)
                || recomendaciones.existeOrdenGeneradaSinCd(tiendaId, varianteId)) {
            return Optional.empty();
        }
        int pronosticado = consultarExistencias.consultarPronosticadoBodega(tiendaId, varianteId).orElse(0);
        if (!Recomendacion.dispara(pronosticado, nivel.get().cantidadMinima())) {
            return Optional.empty();
        }
        UUID cdSugerido = cdsPorPrioridad.cdsPorPrioridad(tiendaId).stream().findFirst().orElse(null);
        return Recomendacion.evaluar(tiendaId, varianteId, pronosticado,
                        nivel.get().cantidadMinima(), nivel.get().cantidadObjetivo(),
                        cdSugerido, clock.instant())
                .map(recomendaciones::guardar);
    }
}
