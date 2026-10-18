package com.codefactory.supplychain.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Lo que ventas necesita saber de una tienda: si está activa y cuál es su Almacén. */
public interface TiendaVentaPort {

    Optional<TiendaVenta> buscar(UUID tiendaId);

    record TiendaVenta(UUID id, boolean activa, UUID almacenId) {
    }
}
