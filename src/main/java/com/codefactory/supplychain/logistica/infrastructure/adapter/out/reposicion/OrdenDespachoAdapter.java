package com.codefactory.supplychain.logistica.infrastructure.adapter.out.reposicion;

import com.codefactory.supplychain.logistica.application.port.out.OrdenDespachoPort;
import com.codefactory.supplychain.logistica.domain.exception.OrdenNoDespachableException;
import com.codefactory.supplychain.reposicion.api.OrdenEstadoInvalidoException;
import com.codefactory.supplychain.reposicion.api.OrdenParaDespacho;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code reposicion.api} para el despacho (HU-46). Traduce el rechazo por estado a una
 * excepción propia de logística; la orden inexistente sigue como 404 de la api.
 *
 * Módulo: logistica.
 */
@Component
@RequiredArgsConstructor
class OrdenDespachoAdapter implements OrdenDespachoPort {

    private final TransicionesOrden transicionesOrden;

    @Override
    public OrdenADespachar iniciarDespacho(UUID ordenId) {
        try {
            OrdenParaDespacho orden = transicionesOrden.iniciarDespacho(ordenId);
            return new OrdenADespachar(orden.cdId(), orden.varianteId(), orden.cantidadSolicitadaCiclo(),
                    orden.reservaRestante());
        } catch (OrdenEstadoInvalidoException e) {
            throw new OrdenNoDespachableException(e.getMessage());
        }
    }

    @Override
    public void confirmarDespacho(UUID ordenId, int cantidadDespachada) {
        transicionesOrden.confirmarDespacho(ordenId, cantidadDespachada);
    }
}
