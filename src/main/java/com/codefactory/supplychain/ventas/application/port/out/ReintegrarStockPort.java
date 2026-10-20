package com.codefactory.supplychain.ventas.application.port.out;

import java.util.UUID;

/** Devuelve al Almacén el stock de una línea anulada. */
public interface ReintegrarStockPort {

    void reintegrar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId);
}
