package com.codefactory.supplychain.logistica.application.port.out;

import com.codefactory.supplychain.logistica.domain.model.Recepcion;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de las recepciones (HU-49).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface RecepcionRepository {

    Optional<Recepcion> buscarPorParada(UUID viajeParadaId);

    /**
     * Guarda y escribe de inmediato. Si la parada ya tiene una recepción, falla por la
     * unicidad de la base antes de que el llamador mueva inventario u orden.
     */
    Recepcion guardar(Recepcion recepcion);
}
