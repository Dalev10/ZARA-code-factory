package com.codefactory.supplychain.inventario.application.port.in.cd;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;

public interface CentroDistribucionUseCase {
    
    CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion);

    CentroDistribucion actualizarCentroDistribucion(int id, String nombre, String ubicacion);

    void eliminarCentroDistribucion(int id);

    CentroDistribucion obtenerCentroDistribucionPorId(int id);

    CentroDistribucion obtenerCentroDistribucionPorNombre(String nombre);

}
