package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.ResolverRecomendacionUseCase.Aprobacion;
import com.codefactory.supplychain.reposicion.application.port.out.RecomendacionRepository;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Una sola transacción abarca la aprobación, la orden, sus
 * intentos y la reserva. Sin reintento: lo hace {@link ResolverRecomendacionRetryableService}.
 * <p>
 * La recomendación se guarda (y su {@code @Version} se comprueba) antes de crear la orden: si otra
 * aprobación ganó, la transacción falla antes de reservar.
 *
 * Módulo: reposicion.
 */
@Service
@Transactional
public class ResolverRecomendacionService {

    private final RecomendacionRepository recomendaciones;
    private final CrearOrdenUseCase crearOrden;
    private final Clock clock;

    public ResolverRecomendacionService(RecomendacionRepository recomendaciones, CrearOrdenUseCase crearOrden,
                                        ObjectProvider<Clock> clock) {
        this.recomendaciones = recomendaciones;
        this.crearOrden = crearOrden;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    public Aprobacion aprobar(UsuarioActual usuario, UUID recomendacionId, Integer cantidadAprobada) {
        Recomendacion aprobada = recomendaciones.guardar(
                cargar(usuario, recomendacionId).aprobar(cantidadAprobada, usuario.id(), clock.instant()));
        OrdenReabastecimiento orden = crearOrden.crear(aprobada.getId(), aprobada.getTiendaId(),
                aprobada.getVarianteId(), aprobada.getCantidadAprobada());
        return new Aprobacion(aprobada, orden);
    }

    public Recomendacion rechazar(UsuarioActual usuario, UUID recomendacionId) {
        return recomendaciones.guardar(cargar(usuario, recomendacionId).rechazar());
    }

    private Recomendacion cargar(UsuarioActual usuario, UUID id) {
        return recomendaciones.buscarPorId(id)
                .filter(recomendacion -> usuario.puedeOperarEn(recomendacion.getTiendaId()))
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.recomendacion(id));
    }
}
