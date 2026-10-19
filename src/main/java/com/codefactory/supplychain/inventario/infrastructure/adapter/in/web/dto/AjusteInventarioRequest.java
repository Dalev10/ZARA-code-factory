package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ajuste de una variante: delta con signo, motivo cerrado y comentario,
 * obligatorio en CORRECCION (D-29).
 */
public record AjusteInventarioRequest(
        @NotBlank String sku,
        @NotNull Integer delta,
        @NotNull
        @Pattern(regexp = "CONTEO_FISICO|INGRESO_MANUAL|MERMA|CORRECCION",
                message = "debe ser CONTEO_FISICO, INGRESO_MANUAL, MERMA o CORRECCION")
        String motivo,
        @Size(max = 500) String comentario) {
}
