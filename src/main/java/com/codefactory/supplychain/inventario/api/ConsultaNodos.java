package com.codefactory.supplychain.inventario.api;

import java.util.Optional;
import java.util.UUID;

public interface ConsultaNodos {
    Optional<UUID> buscarNodoAlmacen(UUID tiendaId);
    Optional<UUID> buscarNodoBodega(UUID tiendaId);
    Optional<UUID> buscarTiendaPorNodoAlmacen(UUID nodoId);
}