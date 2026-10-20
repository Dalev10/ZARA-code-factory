package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.inventario.application.port.in.ResultadoCargaInicial;

import java.util.UUID;

/**
 * Resumen de la carga inicial aplicada.
 */
public record CargaInicialResponse(UUID nodoId, int filasCreadas, int unidadesTotales) {

    public static CargaInicialResponse desde(ResultadoCargaInicial resultado) {
        return new CargaInicialResponse(
                resultado.nodoId(), resultado.filasCreadas(), resultado.unidadesTotales());
    }
}
