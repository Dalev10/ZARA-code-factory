package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;

import java.util.UUID;

/**
 * Crea la orden de reabastecimiento con fallback de CD.
 * <p>
 * Se une a la transacción del llamador: al aprobar la recomendación, una sola
 * transacción abarca la aprobación, la orden, los intentos y la reserva. No
 * reintenta: el reintento por {@code @Version} va fuera de la transacción, en el caso
 * de uso de entrada.
 *
 * Módulo: reposicion.
 */
public interface CrearOrdenUseCase {

    /**
     * Recorre los CDs de la tienda por prioridad y reserva en el primero que cubra toda la
     * cantidad: la orden queda APROBADA con ese CD. Si ninguno la cubre, o la tienda no
     * tiene CDs, queda GENERADA sin CD, con todos los intentos y sin tocar el inventario.
     */
    OrdenReabastecimiento crear(UUID recomendacionId, UUID tiendaId, UUID varianteId, int cantidad);
}
