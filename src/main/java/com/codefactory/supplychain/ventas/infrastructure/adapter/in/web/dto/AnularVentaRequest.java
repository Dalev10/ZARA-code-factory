package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequest(
        @NotBlank @Size(max = 500) String motivo) {
}
