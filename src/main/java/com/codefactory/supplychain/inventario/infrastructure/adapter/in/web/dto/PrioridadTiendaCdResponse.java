package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record PrioridadTiendaCdResponse(UUID id, UUID tiendaId, UUID cdId, int ordenPrioridad) {
}
