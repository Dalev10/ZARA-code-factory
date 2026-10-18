package com.codefactory.supplychain.ventas.application.port.out;

import java.util.UUID;

public interface DescontarStockPort {

    /** @return {@code false} si no hay stock suficiente (y no se forzó). */
    boolean descontar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado);
}
