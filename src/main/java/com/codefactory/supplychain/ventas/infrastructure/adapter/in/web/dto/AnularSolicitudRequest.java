package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo es obligatorio, igual que al anular una venta (D-44). */
public record AnularSolicitudRequest(
        @NotBlank @Size(max = 500) String motivo) {
}
