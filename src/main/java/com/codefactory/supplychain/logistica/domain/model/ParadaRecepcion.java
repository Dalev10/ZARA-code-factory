package com.codefactory.supplychain.logistica.domain.model;

import java.util.UUID;

/**
 * Lo que la recepción necesita saber de una parada de viaje y de su ciclo de despacho (HU-49):
 * a qué tienda llegó, qué orden abastece, cuánto salió del CD y si ya se entregó.
 * <p>
 * Es una vista de solo lectura. La parada, el viaje y el ciclo los gestionan HU-46 a HU-48.
 *
 * @param entregada {@code true} si la parada está ENTREGADO
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public record ParadaRecepcion(
        UUID id,
        UUID viajeId,
        UUID tiendaId,
        UUID cicloDespachoId,
        UUID ordenId,
        int cantidadDespachada,
        boolean entregada) {
}
