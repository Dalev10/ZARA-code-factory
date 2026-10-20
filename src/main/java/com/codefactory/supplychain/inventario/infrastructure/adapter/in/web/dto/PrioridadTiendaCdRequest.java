package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Lista completa de CDs en orden de prioridad para una tienda.
 */
public record PrioridadTiendaCdRequest(
        @NotNull(message = "La lista de CDs es obligatoria")
        List<UUID> cdIds
) {
}
