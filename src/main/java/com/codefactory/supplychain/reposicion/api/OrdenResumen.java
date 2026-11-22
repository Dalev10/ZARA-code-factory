package com.codefactory.supplychain.reposicion.api;

import java.util.UUID;

/**
 * Resumen estable de una orden de reabastecimiento para otros módulos (sección 4.2).
 *
 * @param cdId     CD asignado; nulo si la orden está GENERADA sin CD
 * @param estado   nombre del estado de la orden (sección 2.3), p. ej. {@code "APROBADA"}
 */
public record OrdenResumen(
        UUID id,
        UUID tiendaId,
        UUID cdId,
        UUID varianteId,
        String estado,
        int cantidadSolicitada,
        int cantidadPendiente) {
}
