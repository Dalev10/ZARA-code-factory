package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado cuando una reposición devuelve a positivo un stock que
 * estaba agotado.
 */
public record ExistenciasRepuestas(
        UUID nodoId,
        UUID varianteId,
        Instant fecha) {
}
