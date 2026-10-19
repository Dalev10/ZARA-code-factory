package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarExistenciasPort {
    Optional<Integer> consultarStockAlmacen(UUID tiendaId, UUID varianteId);
    Optional<Integer> consultarStockBodega(UUID tiendaId, UUID varianteId);
    /** Variantes con fila en el Almacén o en la Bodega de la tienda: las que un umbral puede reponer. */
    List<UUID> variantesConInventario(UUID tiendaId);
}