package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;

import java.time.Instant;
import java.util.UUID;

/**
 * Representación pública de un traspaso interno.
 *
 * Módulo: reposicion — Reposición interna.
 */
public record TraspasoInternoResponse(
        UUID id,
        UUID varianteId,
        UUID tiendaId,
        int cantidad,
        OrigenTraspaso origen,
        UUID registradoPor,
        Instant timestampTraspaso
) {
}
