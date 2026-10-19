package com.codefactory.supplychain.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Resolución del nodo Almacén de una tienda. Respaldado por inventario.api.ConsultaNodos. */
public interface AlmacenTiendaPort {

    Optional<UUID> almacenDeTienda(UUID tiendaId);
}
