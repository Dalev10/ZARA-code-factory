package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Un ítem del lote: SKU y cantidad absoluta. El 0 registra "se contó y no
 * había" (D-30).
 */
public record ItemCargaInicialRequest(
        @NotBlank String sku,
        @NotNull @PositiveOrZero Integer cantidad) {
}
