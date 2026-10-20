package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.domain.model.Inventario;

import java.util.UUID;

/**
 * Existencias de una variante en un nodo.
 */
public record StockResponse(
        UUID nodoId,
        UUID varianteId,
        int aLaMano,
        int disponibleParaUso,
        int entrante,
        int saliente,
        int pronosticado) {

    public static StockResponse desde(Inventario inventario) {
        return new StockResponse(
                inventario.getNodoId(),
                inventario.getVarianteId(),
                inventario.getALaMano(),
                inventario.getDisponible(),
                inventario.getEntrante(),
                inventario.getSaliente(),
                inventario.getPronosticado());
    }
}
