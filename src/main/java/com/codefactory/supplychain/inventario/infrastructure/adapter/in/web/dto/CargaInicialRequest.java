package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lote de carga inicial de un nodo: de 1 a 500 ítems (D-30).
 */
public record CargaInicialRequest(
        @NotEmpty @Size(max = 500) List<@Valid ItemCargaInicialRequest> items) {
}
