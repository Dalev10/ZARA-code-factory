package com.codefactory.supplychain.logistica.infrastructure.adapter.out.reposicion;

import com.codefactory.supplychain.logistica.application.port.out.OrdenViajePort;
import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code reposicion.api} para los viajes (HU-47).
 *
 * Módulo: logistica.
 */
@Component
@RequiredArgsConstructor
class OrdenViajeAdapter implements OrdenViajePort {

    private final ConsultaOrdenes consultaOrdenes;

    @Override
    public DestinoOrden consultar(UUID ordenId) {
        OrdenResumen orden = consultaOrdenes.consultarOrden(ordenId);
        return new DestinoOrden(orden.cdId(), orden.tiendaId());
    }
}
