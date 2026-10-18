package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarExistenciasPort {
    Optional<Integer> consultarStockAlmacen(UUID tiendaId, UUID varianteId);
    Optional<Integer> consultarStockBodega(UUID tiendaId, UUID varianteId);
    List<UUID> variantesConInventarioEnAlmacen(UUID tiendaId);
}