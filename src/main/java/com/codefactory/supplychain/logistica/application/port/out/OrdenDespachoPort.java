package com.codefactory.supplychain.logistica.application.port.out;

import java.util.UUID;

/** Transiciones de la orden que pide el despacho. Respaldado por {@code reposicion.api}. */
public interface OrdenDespachoPort {

    /**
     * APROBADA o REABIERTA → EN_DESPACHO.
     *
     * @throws com.codefactory.supplychain.logistica.domain.exception.OrdenNoDespachableException en otro estado
     */
    OrdenADespachar iniciarDespacho(UUID ordenId);

    /** EN_DESPACHO → DESPACHADA, acumulando lo despachado. */
    void confirmarDespacho(UUID ordenId, int cantidadDespachada);

    /**
     * @param cantidadSolicitadaCiclo el pendiente de la orden
     * @param reservaRestante         la reserva que la orden aún tiene en el CD
     */
    record OrdenADespachar(UUID cdId, UUID varianteId, int cantidadSolicitadaCiclo, int reservaRestante) {
    }
}
