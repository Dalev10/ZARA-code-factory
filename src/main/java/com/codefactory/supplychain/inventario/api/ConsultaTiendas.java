package com.codefactory.supplychain.inventario.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta de tiendas para otros módulos (solo lectura). Consumidores: identity (HU-32,
 * existencia al asignar tiendas a usuarios) y ventas (HU-40/43, tienda activa).
 */
public interface ConsultaTiendas {

    Optional<TiendaResumen> buscar(UUID tiendaId);

    /**
     * Las que no existen se omiten; el orden del resultado no está garantizado.
     */
    List<TiendaResumen> buscarTodas(Collection<UUID> tiendaIds);
}
