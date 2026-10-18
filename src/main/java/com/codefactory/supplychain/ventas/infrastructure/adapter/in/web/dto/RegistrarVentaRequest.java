package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Vendedor, origen y hora no vienen en el body: los fija el servidor (D-10, D-38). */
public record RegistrarVentaRequest(
        @NotNull UUID transaccionId,
        @NotNull UUID tiendaId,
        @Size(max = 30) String canal,
        boolean forzarSinStock,
        @NotEmpty @Size(max = 200) List<@Valid ItemRequest> items) {

    public record ItemRequest(
            @NotBlank String sku,
            @Positive int cantidad,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario) {
    }
}
