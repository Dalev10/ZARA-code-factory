package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.api.StockResumen;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de lectura de existencias (tabla inventario) usado por
 * {@code ConsultaInventarioService}.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface ExistenciasRepositoryPort {

    Optional<StockResumen> buscarPorNodoYVariante(UUID nodoId, UUID varianteId);

    List<StockResumen> listarPorNodo(UUID nodoId);
}
