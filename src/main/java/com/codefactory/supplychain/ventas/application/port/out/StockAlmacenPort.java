package com.codefactory.supplychain.ventas.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stock del Almacén (HU-34). Respaldado por inventario.api.ConsultaInventario. */
public interface StockAlmacenPort {

    /**
     * {@code a la mano} del Almacén. Vacío si la variante no tiene carga inicial (no equivale a cero).
     *
     * @throws com.codefactory.supplychain.ventas.domain.exception.IntegracionInventarioPendienteException si Inventario no implementa el contrato
     */
    Optional<Integer> aLaMano(UUID almacenId, UUID varianteId);

    /** Variantes con existencias cargadas en el Almacén. Misma excepción de integración pendiente. */
    List<UUID> variantesConExistencias(UUID almacenId);
}
