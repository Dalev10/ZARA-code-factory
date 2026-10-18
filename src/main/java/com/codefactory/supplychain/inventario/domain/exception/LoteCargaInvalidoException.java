package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

import java.util.List;

/**
 * El lote de carga inicial no cumple D-30: vacío, más de 500 ítems,
 * cantidades negativas, SKU repetidos o SKU que no existen en el catálogo.
 *
 * TODO(shared): exponer {@link #getSkus()} como campo estructurado cuando
 * ErrorResponse admita detalle; hoy la lista solo viaja en el mensaje.
 */
public class LoteCargaInvalidoException extends ReglaDeNegocioException {

    private final List<String> skus;

    public LoteCargaInvalidoException(String mensaje) {
        super(mensaje);
        this.skus = List.of();
    }

    private LoteCargaInvalidoException(String mensaje, List<String> skus) {
        super(mensaje + ": " + String.join(", ", skus));
        this.skus = List.copyOf(skus);
    }

    public static LoteCargaInvalidoException skusRepetidos(List<String> skus) {
        return new LoteCargaInvalidoException("SKU repetidos en el lote", skus);
    }

    public static LoteCargaInvalidoException skusInexistentes(List<String> skus) {
        return new LoteCargaInvalidoException("SKU inexistentes en el catálogo", skus);
    }

    public List<String> getSkus() {
        return skus;
    }
}
