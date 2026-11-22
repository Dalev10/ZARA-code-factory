package com.codefactory.supplychain.reposicion.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un CD evaluado durante el fallback de la orden: su posición en la lista
 * de prioridad, el resultado y, si se descartó, el motivo (disponible frente a requerido).
 *
 * Módulo: reposicion.
 */
public record IntentoCd(UUID id, UUID cdId, int ordenEvaluado, ResultadoIntentoCd resultado,
                        String motivo, Instant timestamp) {

    public IntentoCd {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cdId, "cdId");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(timestamp, "timestamp");
        if (ordenEvaluado <= 0) {
            throw new IllegalArgumentException("La posición evaluada empieza en 1");
        }
    }

    static IntentoCd sinStock(UUID cdId, int ordenEvaluado, int disponible, int requerido, Instant ahora) {
        return new IntentoCd(UUID.randomUUID(), cdId, ordenEvaluado, ResultadoIntentoCd.SIN_STOCK,
                "Disponible " + disponible + " menor que lo requerido " + requerido, ahora);
    }

    static IntentoCd asignado(UUID cdId, int ordenEvaluado, Instant ahora) {
        return new IntentoCd(UUID.randomUUID(), cdId, ordenEvaluado, ResultadoIntentoCd.ASIGNADO, null, ahora);
    }
}
