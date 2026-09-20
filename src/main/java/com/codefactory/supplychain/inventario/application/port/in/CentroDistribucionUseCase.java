package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface CentroDistribucionUseCase {

    CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion);

    CentroDistribucion actualizarCentroDistribucion(UUID id, String nombre, String ubicacion);

    void eliminarCentroDistribucion(UUID id);

    CentroDistribucion obtenerCentroDistribucionPorId(UUID id);

    CentroDistribucion obtenerCentroDistribucionPorNombre(String nombre);

    Page<CentroDistribucion> buscarCentrosDistribucion(UUID id, String nombre, String ubicacion, Pageable pageable);

    CentroDistribucionConNodo buscarCentroDistribucionConNodo(UUID id);
}
