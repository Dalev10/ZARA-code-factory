package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;

import java.util.Optional;
import java.util.UUID;

/**
 * Evaluación de la reposición CD → Bodega_Tienda de un SKU en una tienda.
 *
 * Módulo: reposicion.
 */
public interface EvaluarRecomendacionUseCase {

    /**
     * Idempotente: crea una recomendación BORRADOR si el pronosticado de la Bodega
     * está bajo el mínimo del nivel, y no hay ya un BORRADOR ni una orden GENERADA
     * sin CD para el SKU y la tienda.
     *
     */
    Optional<Recomendacion> evaluar(UUID tiendaId, UUID varianteId);
}
