package com.codefactory.supplychain.reposicion.api;

import java.util.UUID;

/**
 * Datos que logística necesita para despachar un ciclo de la orden.
 *
 * @param cantidadSolicitadaCiclo {@code cantidad_pendiente} de la orden
 * @param reservaRestante         {@code max(cantidad_total_solicitada − cantidad_despachada_acumulada, 0)}
 */
public record OrdenParaDespacho(
        UUID cdId,
        UUID tiendaId,
        UUID varianteId,
        int cantidadSolicitadaCiclo,
        int reservaRestante) {
}
