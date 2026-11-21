package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cuerpo de {@code POST /viajes/{viajeId}/paradas/{paradaId}/recepcion} (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Schema(description = "Lo que llegó a la Bodega_Tienda en la parada")
public record RegistrarRecepcionRequest(
        @Schema(description = "Unidades recibidas. Entre 0 y lo despachado en la parada; lo que falte queda "
                + "como discrepancia (pérdida en tránsito)", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad recibida es obligatoria")
        @PositiveOrZero(message = "La cantidad recibida no puede ser negativa")
        Integer cantidadRecibida) {
}
