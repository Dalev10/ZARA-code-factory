package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Inventario;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del agregado Inventario.
 */
public interface InventarioRepositoryPort {

    Optional<Inventario> buscarPorNodoYVariante(UUID nodoId, UUID varianteId);

    Inventario guardar(Inventario inventario);
}
