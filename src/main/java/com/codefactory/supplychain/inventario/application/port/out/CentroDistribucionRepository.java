package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface CentroDistribucionRepository {

    CentroDistribucion guardar(CentroDistribucion centroDistribucion);

    Optional<CentroDistribucion> buscarPorId(UUID id);

    Optional<CentroDistribucion> buscarPorNombre(String nombre);

    Page<CentroDistribucion> buscar(UUID id, String nombre, String ubicacion, Pageable pageable);

    void eliminar(UUID id);
}
