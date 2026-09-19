package com.codefactory.supplychain.inventario.application.port.out.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import java.util.List;

public interface CentroDistribucionRepository{

    public CentroDistribucion findById(int id);

    public CentroDistribucion findByNombre(String nombre);

    public List<CentroDistribucion> buscar(Integer id, String nombre, String ubicacion);

    public CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion);

    public CentroDistribucion actualizarCentroDistribucion(CentroDistribucion centroDistribucion);

    public void eliminarCentroDistribucion(int id);

    CentroDistribucionConNodo buscarPorIdConNodo(int id);
}
