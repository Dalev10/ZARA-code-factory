package com.codefactory.supplychain.inventario.application.port.in.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import java.util.List;

public interface CentroDistribucionUseCase {
    
    CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion);

    CentroDistribucion actualizarCentroDistribucion(int id, String nombre, String ubicacion);

    void eliminarCentroDistribucion(int id);

    CentroDistribucion obtenerCentroDistribucionPorId(int id);

    CentroDistribucion obtenerCentroDistribucionPorNombre(String nombre);

    List<CentroDistribucion> buscarCentrosDistribucion(Integer id, String nombre, String ubicacion);

}
