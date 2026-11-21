package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.application.dto.ExistenciaConsulta;
import com.codefactory.supplychain.inventario.domain.model.Inventario;

import java.util.UUID;

/**
 * Existencias de una variante en un nodo.
 */
public record StockResponse(
        UUID nodoId,
        UUID varianteId,
        String sku,
        int aLaMano,
        int disponibleParaUso,
        int entrante,
        int saliente,
        int pronosticado) {

    public static StockResponse desde(ExistenciaConsulta existencia) {
        Inventario inventario = existencia.inventario();
        return new StockResponse(
                inventario.getNodoId(),
                inventario.getVarianteId(),
                existencia.sku(),
                inventario.getALaMano(),
                inventario.getDisponible(),
                inventario.getEntrante(),
                inventario.getSaliente(),
                inventario.getPronosticado());
    }
}
