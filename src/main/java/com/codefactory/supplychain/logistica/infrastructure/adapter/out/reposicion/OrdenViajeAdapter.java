package com.codefactory.supplychain.logistica.infrastructure.adapter.out.reposicion;

import com.codefactory.supplychain.logistica.application.port.out.OrdenViajePort;
import com.codefactory.supplychain.logistica.domain.exception.OrdenNoAdmiteSalidaException;
import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenEstadoInvalidoException;
import com.codefactory.supplychain.reposicion.api.OrdenResumen;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code reposicion.api} para los viajes (HU-47) y su salida (HU-48).
 *
 * Módulo: logistica.
 */
@Component
@RequiredArgsConstructor
class OrdenViajeAdapter implements OrdenViajePort {

    private final ConsultaOrdenes consultaOrdenes;
    private final TransicionesOrden transicionesOrden;

    @Override
    public DestinoOrden consultar(UUID ordenId) {
        OrdenResumen orden = consultaOrdenes.consultarOrden(ordenId);
        return new DestinoOrden(orden.cdId(), orden.tiendaId());
    }

    @Override
    public void marcarEnTransito(UUID ordenId) {
        try {
            transicionesOrden.marcarEnTransito(ordenId);
        } catch (OrdenEstadoInvalidoException e) {
            throw new OrdenNoAdmiteSalidaException(e.getMessage());
        }
    }
}
