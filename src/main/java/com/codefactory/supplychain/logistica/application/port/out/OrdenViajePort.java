package com.codefactory.supplychain.logistica.application.port.out;

import java.util.UUID;

/** Lo que el viaje necesita de la orden de cada ciclo. Respaldado por {@code reposicion.api}. */
public interface OrdenViajePort {

    /** CD y tienda de la orden. */
    DestinoOrden consultar(UUID ordenId);

    record DestinoOrden(UUID cdId, UUID tiendaId) {
    }
}
