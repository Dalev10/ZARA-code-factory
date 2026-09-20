package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;

import java.util.List;
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

    List<CentroDistribucion> buscarCentrosDistribucion(UUID id, String nombre, String ubicacion);

    CentroDistribucionConNodo buscarCentroDistribucionConNodo(UUID id);
}
