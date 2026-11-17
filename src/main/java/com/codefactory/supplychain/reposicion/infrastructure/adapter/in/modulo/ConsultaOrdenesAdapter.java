package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenNoEncontradaException;
import com.codefactory.supplychain.reposicion.api.OrdenResumen;
import com.codefactory.supplychain.reposicion.application.port.in.ConsultarOrdenesUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Implementa {@link ConsultaOrdenes} sobre {@link ConsultarOrdenesUseCase} y traduce
 * la orden de dominio a {@link OrdenResumen}.
 *
 * Módulo: reposicion — HU-45.
 */
@Component
class ConsultaOrdenesAdapter implements ConsultaOrdenes {

    private final ConsultarOrdenesUseCase consultarOrdenes;

    ConsultaOrdenesAdapter(ConsultarOrdenesUseCase consultarOrdenes) {
        this.consultarOrdenes = consultarOrdenes;
    }

    @Override
    public OrdenResumen consultarOrden(UUID ordenId) {
        Objects.requireNonNull(ordenId, "ordenId es obligatorio");
        return consultarOrdenes.buscar(ordenId)
                .map(ConsultaOrdenesAdapter::aResumen)
                .orElseThrow(() -> new OrdenNoEncontradaException(ordenId));
    }

    private static OrdenResumen aResumen(OrdenReabastecimiento orden) {
        return new OrdenResumen(orden.getId(), orden.getTiendaId(), orden.getCdAsignadoId(), orden.getVarianteId(),
                orden.getEstado().name(), orden.getCantidadTotalSolicitada(), orden.getCantidadPendiente());
    }
}
