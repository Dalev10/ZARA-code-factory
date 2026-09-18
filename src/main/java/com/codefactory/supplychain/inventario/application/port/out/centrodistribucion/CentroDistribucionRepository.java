package com.codefactory.supplychain.inventario.application.port.out.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;

public interface CentroDistribucionRepository{

    public CentroDistribucion findById(int id);

    public CentroDistribucion findByNombre(String nombre);

    public CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion);

    public CentroDistribucion actualizarCentroDistribucion(CentroDistribucion centroDistribucion);

    public void eliminarCentroDistribucion(int id);
}
