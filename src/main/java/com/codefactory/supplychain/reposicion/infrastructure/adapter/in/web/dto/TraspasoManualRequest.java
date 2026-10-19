package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Cuerpo del POST para registrar un traspaso manual.
 * <p>
 * {@code registradoPor} NO viene aquí: se extrae del token JWT.
 *
 * Módulo: reposicion — Reposición interna.
 */
public record TraspasoManualRequest(

        @NotNull(message = "La variante es obligatoria")
        UUID varianteId,

        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        int cantidad
) {
}
