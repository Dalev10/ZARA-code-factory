package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** {@code varianteId}, {@code desde} y {@code hasta} son opcionales. */
public record ReconciliarVentanasRequest(@NotNull UUID tiendaId, UUID varianteId, Instant desde, Instant hasta) {
}
