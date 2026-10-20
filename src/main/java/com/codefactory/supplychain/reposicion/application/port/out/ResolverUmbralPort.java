package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve el umbral que aplica a una variante en una tienda (D-58). Hoy el umbral es
 * constante: la tienda se recibe pero se ignora, para que el Sprint 4 pueda hacerlo
 * dinámico sin cambiar el contrato ni a quien lo usa (el traspaso automático, HU-48).
 */
public interface ResolverUmbralPort {

    /**
     * Precedencia: umbral de la variante, y si no existe, el de su categoría.
     *
     * @return vacío si no hay umbral aplicable: entonces no hay reposición automática.
     */
    Optional<UmbralAplicable> resolver(UUID tiendaId, UUID varianteId);

    /** Mínimo y objetivo vigentes para la variante en la tienda. */
    record UmbralAplicable(int cantidadMinima, int cantidadObjetivo) {
    }
}
