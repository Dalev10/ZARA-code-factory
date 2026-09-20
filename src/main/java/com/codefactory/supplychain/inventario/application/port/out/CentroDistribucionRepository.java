package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface CentroDistribucionRepository {

    CentroDistribucion guardar(CentroDistribucion centroDistribucion);

    Optional<CentroDistribucion> buscarPorId(UUID id);

    Optional<CentroDistribucion> buscarPorNombre(String nombre);

    List<CentroDistribucion> buscar(UUID id, String nombre, String ubicacion);

    void eliminar(UUID id);
}
