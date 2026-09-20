package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.application.port.in.CentroDistribucionUseCase;
import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionDuplicadoException;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Service
@RequiredArgsConstructor
public class CentroDistribucionService implements CentroDistribucionUseCase {

    private final CentroDistribucionRepository centroDistribucionRepository;
    private final NodoRepositoryPort nodoRepositoryPort;

    @Override
    @Transactional
    public CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion) {
        if (centroDistribucionRepository.buscarPorNombre(nombre).isPresent()) {
            throw new CentroDistribucionDuplicadoException(
                    "El centro de distribución con el nombre '" + nombre + "' ya existe.");
        }
        CentroDistribucion creado = centroDistribucionRepository.guardar(CentroDistribucion.crear(nombre, ubicacion));
        nodoRepositoryPort.guardar(Nodo.crearParaCd(creado.getId()));
        return creado;
    }

    @Override
    @Transactional
    public CentroDistribucion actualizarCentroDistribucion(UUID id, String nombre, String ubicacion) {
        obtenerCentroDistribucionPorId(id);

        centroDistribucionRepository.buscarPorNombre(nombre)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new CentroDistribucionDuplicadoException(
                            "El centro de distribución con el nombre '" + nombre + "' ya existe.");
                });

        return centroDistribucionRepository.guardar(new CentroDistribucion(id, nombre, ubicacion));
    }

    @Override
    @Transactional
    public void eliminarCentroDistribucion(UUID id) {
        obtenerCentroDistribucionPorId(id);
        // NOTA: no elimina el Nodo asociado (tipo=CD) — deuda técnica conocida,
        // registrada para resolverse cuando se retome FEAT-04 de lleno. Hoy
        // este DELETE falla por la FK de nodo.cd_id si el CD ya tiene su nodo
        // (que siempre lo tiene, dado que crearCentroDistribucion lo aprovisiona).
        centroDistribucionRepository.eliminar(id);
    }

    @Override
    public CentroDistribucion obtenerCentroDistribucionPorId(UUID id) {
        return centroDistribucionRepository.buscarPorId(id)
                .orElseThrow(() -> new CentroDistribucionNoEncontradoException(
                        "No existe un centro de distribución con el id " + id));
    }

    @Override
    public CentroDistribucion obtenerCentroDistribucionPorNombre(String nombre) {
        return centroDistribucionRepository.buscarPorNombre(nombre)
                .orElseThrow(() -> new CentroDistribucionNoEncontradoException(
                        "No existe un centro de distribución con el nombre " + nombre));
    }

    @Override
    public List<CentroDistribucion> buscarCentrosDistribucion(UUID id, String nombre, String ubicacion) {
        if (id == null && (nombre == null || nombre.isBlank()) && (ubicacion == null || ubicacion.isBlank())) {
            throw new IllegalArgumentException(
                    "Debe proporcionar al menos un parámetro de búsqueda (id, nombre o ubicación).");
        }
        return centroDistribucionRepository.buscar(id, nombre, ubicacion);
    }

    @Override
    public CentroDistribucionConNodo buscarCentroDistribucionConNodo(UUID id) {
        CentroDistribucion centroDistribucion = obtenerCentroDistribucionPorId(id);
        Nodo nodo = nodoRepositoryPort.buscarPorCdId(id).orElse(null);
        return new CentroDistribucionConNodo(centroDistribucion, nodo);
    }
}
