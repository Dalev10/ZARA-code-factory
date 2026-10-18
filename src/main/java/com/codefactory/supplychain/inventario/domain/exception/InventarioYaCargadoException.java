package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.List;
import java.util.UUID;

/**
 * Indica que ya existe una carga inicial para el nodo y la variante (D-28).
 *
 * TODO(shared): exponer {@link #getSkus()} como campo estructurado cuando
 * ErrorResponse admita detalle; hoy la lista solo viaja en el mensaje.
 */
public class InventarioYaCargadoException extends RecursoDuplicadoException {

    private final List<String> skus;

    public InventarioYaCargadoException(UUID nodoId, UUID varianteId) {
        super("Ya existe inventario cargado para la variante " + varianteId
                + " en el nodo " + nodoId);
        this.skus = List.of();
    }

    public InventarioYaCargadoException(UUID nodoId, List<String> skus) {
        super("Variantes ya cargadas en el nodo " + nodoId + ": " + String.join(", ", skus));
        this.skus = List.copyOf(skus);
    }

    public List<String> getSkus() {
        return skus;
    }
}
