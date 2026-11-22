package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Cuerpo de POST /viajes (HU-47). {@code ordenParada} es opcional: por defecto, la posición en la lista. */
public record ArmarViajeRequest(
        @NotNull UUID cdId,
        @NotBlank @Size(max = 100) String vehiculo,
        @NotEmpty @Valid List<Parada> paradas) {

    public record Parada(@NotNull UUID cicloDespachoId, @Positive Integer ordenParada) {
    }
}
