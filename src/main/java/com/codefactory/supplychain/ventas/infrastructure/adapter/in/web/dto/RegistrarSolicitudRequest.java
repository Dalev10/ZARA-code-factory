package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** El usuario que registra NO viaja en el cuerpo: sale del token. {@code cantidad} omitida equivale a 1. */
public record RegistrarSolicitudRequest(
        @NotNull UUID tiendaId,
        @NotNull UUID varianteId,
        @Positive Integer cantidad,
        @NotBlank @Size(max = 100) String claveIdempotencia) {
}
