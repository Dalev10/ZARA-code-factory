package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio para cerrar con faltante o cancelar una orden (HU-53). */
public record MotivoCierreRequest(
        @Schema(example = "El proveedor confirmó que no hay reposición de esta referencia")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo) {
}
