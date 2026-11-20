package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Nivel de reposición al CD que aplica a una variante en una tienda.
 * Precedencia: variante → categoría.
 * <p>
 *
 * Módulo: reposicion.
 */
public interface ResolverNivelPort {

    Optional<NivelAplicable> resolver(UUID tiendaId, UUID varianteId);

    record NivelAplicable(int cantidadMinima, int cantidadObjetivo) {
    }
}
