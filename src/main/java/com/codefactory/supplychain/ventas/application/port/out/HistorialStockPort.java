package com.codefactory.supplychain.ventas.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Historial de movimientos del Almacén para reconciliar (HU-46, D-49). Respaldado por inventario.api.ConsultaKardex. */
public interface HistorialStockPort {

    /** {@code cantidad} es el delta aplicado; {@code saldoResultante} el a-la-mano posterior al movimiento. */
    record CambioStock(UUID movimientoId, String tipo, int cantidad, int saldoResultante, Instant fecha) {
    }

    /** Movimientos entre {@code desde} y {@code hasta} (inclusive), del más antiguo al más reciente. */
    List<CambioStock> cambios(UUID almacenId, UUID varianteId, Instant desde, Instant hasta);
}
