package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.reposicion.api.OrdenEstadoInvalidoException;
import com.codefactory.supplychain.reposicion.api.OrdenNoEncontradaException;
import com.codefactory.supplychain.reposicion.api.OrdenParaDespacho;
import com.codefactory.supplychain.reposicion.api.ResultadoRecepcion;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import com.codefactory.supplychain.reposicion.application.port.in.TransicionesOrdenUseCase;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.TransicionOrdenInvalidaException;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Implementa {@link TransicionesOrden} sobre {@link TransicionesOrdenUseCase} y traduce
 * las excepciones del dominio a las de la api.
 *
 * Módulo: reposicion.
 */
@Component
class TransicionesOrdenAdapter implements TransicionesOrden {

    private final TransicionesOrdenUseCase transiciones;

    TransicionesOrdenAdapter(TransicionesOrdenUseCase transiciones) {
        this.transiciones = transiciones;
    }

    @Override
    public OrdenParaDespacho iniciarDespacho(UUID ordenId) {
        OrdenReabastecimiento orden = traducir(ordenId, () -> transiciones.iniciarDespacho(ordenId));
        return new OrdenParaDespacho(orden.getCdAsignadoId(), orden.getTiendaId(), orden.getVarianteId(),
                orden.getCantidadPendiente(), orden.reservaRestante());
    }

    @Override
    public void confirmarDespacho(UUID ordenId, int cantidadDespachada) {
        traducir(ordenId, () -> transiciones.confirmarDespacho(ordenId, cantidadDespachada));
    }

    @Override
    public void marcarEnTransito(UUID ordenId) {
        traducir(ordenId, () -> transiciones.marcarEnTransito(ordenId));
    }

    @Override
    public ResultadoRecepcion registrarRecepcion(UUID ordenId, int cantidadRecibida) {
        OrdenReabastecimiento orden = traducir(ordenId, () -> transiciones.registrarRecepcion(ordenId, cantidadRecibida));
        return new ResultadoRecepcion(orden.getEstado().name(), orden.getCantidadPendiente());
    }

    private static OrdenReabastecimiento traducir(UUID ordenId, Supplier<OrdenReabastecimiento> operacion) {
        Objects.requireNonNull(ordenId, "ordenId es obligatorio");
        try {
            return operacion.get();
        } catch (ReposicionRecursoNoEncontradoException e) {
            throw new OrdenNoEncontradaException(ordenId);
        } catch (TransicionOrdenInvalidaException e) {
            throw new OrdenEstadoInvalidoException(e.getMessage());
        }
    }
}
