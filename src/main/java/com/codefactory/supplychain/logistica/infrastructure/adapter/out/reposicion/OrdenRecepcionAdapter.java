package com.codefactory.supplychain.logistica.infrastructure.adapter.out.reposicion;

import com.codefactory.supplychain.logistica.application.port.out.OrdenRecepcionPort;
import com.codefactory.supplychain.logistica.domain.exception.OrdenNoAdmiteRecepcionException;
import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenEstadoInvalidoException;
import com.codefactory.supplychain.reposicion.api.OrdenResumen;
import com.codefactory.supplychain.reposicion.api.ResultadoRecepcion;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code reposicion.api}: consulta la orden de la parada y le registra lo recibido.
 * Traduce el rechazo por estado a una excepción propia de logística.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Component
@RequiredArgsConstructor
class OrdenRecepcionAdapter implements OrdenRecepcionPort {

    private final ConsultaOrdenes consultaOrdenes;
    private final TransicionesOrden transicionesOrden;

    @Override
    public Orden consultar(UUID ordenId) {
        OrdenResumen orden = consultaOrdenes.consultarOrden(ordenId);
        return new Orden(orden.varianteId(), new Situacion(orden.estado(), orden.cantidadPendiente()));
    }

    @Override
    public Situacion registrarRecepcion(UUID ordenId, int cantidadRecibida) {
        try {
            ResultadoRecepcion resultado = transicionesOrden.registrarRecepcion(ordenId, cantidadRecibida);
            return new Situacion(resultado.estadoFinal(), resultado.cantidadPendiente());
        } catch (OrdenEstadoInvalidoException e) {
            throw new OrdenNoAdmiteRecepcionException(e.getMessage());
        }
    }
}
