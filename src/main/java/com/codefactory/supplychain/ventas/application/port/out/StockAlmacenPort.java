package com.codefactory.supplychain.ventas.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stock del Almacén (HU-43, HU-46). Respaldado por inventario.api.ConsultaInventario. */
public interface StockAlmacenPort {

    /** {@code a la mano} del Almacén. Vacío si la variante nunca tuvo fila en él (su stock es 0, D-25). */
    Optional<Integer> aLaMano(UUID almacenId, UUID varianteId);

    /** Variantes con fila en el Almacén, incluidas las de saldo cero o negativo. */
    List<UUID> variantesConExistencias(UUID almacenId);
}
