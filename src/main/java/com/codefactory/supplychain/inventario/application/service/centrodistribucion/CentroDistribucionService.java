package com.codefactory.supplychain.inventario.application.service.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.application.port.out.centrodistribucion.CentroDistribucionRepository;
import org.springframework.stereotype.Service;
import com.codefactory.supplychain.inventario.domain.exception.centrodistribucion.CentroDistribucionDuplicado;
import com.codefactory.supplychain.inventario.domain.exception.centrodistribucion.CentroDistribucionNoEncontrado;
import com.codefactory.supplychain.inventario.application.port.in.centrodistribucion.CentroDistribucionUseCase;
import java.util.List;

@Service 
public class CentroDistribucionService implements CentroDistribucionUseCase {
    private CentroDistribucionRepository centroDistribucionRepository;

    public CentroDistribucionService(CentroDistribucionRepository centroDistribucionRepository) {
        this.centroDistribucionRepository = centroDistribucionRepository;
    }

    public CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion) {

        CentroDistribucion existingCentroDistribucion = centroDistribucionRepository.findByNombre(nombre);
        if (existingCentroDistribucion != null) {
            throw new CentroDistribucionDuplicado("El centro de distribución con el nombre '" + nombre + "' ya existe.");
        }

        return centroDistribucionRepository.crearCentroDistribucion(nombre, ubicacion);
    }

    public CentroDistribucion actualizarCentroDistribucion(int id, String nombre, String ubicacion) {

        CentroDistribucion centroDistribucionExistente = centroDistribucionRepository.findById(id);
        if (centroDistribucionExistente == null) {
            throw new CentroDistribucionNoEncontrado("No existe un centro de distribución con el id " + id);
        }

        CentroDistribucion centroDistribucionConMismoNombre = centroDistribucionRepository.findByNombre(nombre);
        if (centroDistribucionConMismoNombre != null && centroDistribucionConMismoNombre.getId() != id) {
            throw new CentroDistribucionDuplicado("El centro de distribución con el nombre '" + nombre + "' ya existe.");
        }

        CentroDistribucion centroDistribucionActualizado = new CentroDistribucion(id, nombre, ubicacion);


        return centroDistribucionRepository.actualizarCentroDistribucion(centroDistribucionActualizado);
    }

    public void eliminarCentroDistribucion(int id) {
        CentroDistribucion centroDistribucionExistente = centroDistribucionRepository.findById(id);
        if (centroDistribucionExistente == null) {
            throw new CentroDistribucionNoEncontrado("No existe un centro de distribución con el id " + id);
        }

        centroDistribucionRepository.eliminarCentroDistribucion(id);
    }

    public CentroDistribucion obtenerCentroDistribucionPorId(int id) {
        
        CentroDistribucion centro = centroDistribucionRepository.findById(id);

        if (centro == null) {
            throw new CentroDistribucionNoEncontrado(
                    "No existe un centro de distribución con el id " + id
            );
        }

        return centro;
    }

    public CentroDistribucion obtenerCentroDistribucionPorNombre(String nombre) {
        CentroDistribucion centro = centroDistribucionRepository.findByNombre(nombre);

        if (centro == null) {
            throw new CentroDistribucionNoEncontrado(
                    "No existe un centro de distribución con el nombre " + nombre
            );
        }

        return centro;
    }

    public List<CentroDistribucion> buscarCentrosDistribucion(Integer id, String nombre, String ubicacion) {

        if (id == null && (nombre == null || nombre.isBlank()) && (ubicacion == null || ubicacion.isBlank())) {

            throw new IllegalArgumentException(
                    "Debe proporcionar al menos un parámetro de búsqueda (id, nombre o ubicación)."
            );
        }

        return centroDistribucionRepository.buscar(id, nombre, ubicacion);

    }

}
