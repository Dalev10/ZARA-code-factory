package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.TiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Crea una Tienda y su Nodo Almacén satélite en la misma operación — toda
 * Tienda tiene su Almacén desde que existe. El Nodo Bodega_Tienda se
 * aprovisiona por separado (ver BodegaTiendaService, FEAT-03) sobre una
 * Tienda ya existente.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Service
@RequiredArgsConstructor
public class TiendaService implements TiendaUseCase {

    private final TiendaRepositoryPort tiendaRepositoryPort;
    private final NodoRepositoryPort nodoRepositoryPort;

    @Override
    @Transactional
    public Tienda crear(String nombre, String ubicacion) {
        if (tiendaRepositoryPort.existePorNombre(nombre)) {
            throw new TiendaYaExisteException();
        }
        Tienda creada = tiendaRepositoryPort.guardar(Tienda.crear(nombre, ubicacion));
        nodoRepositoryPort.guardar(Nodo.crearParaTienda(creada.getId(), TipoNodo.ALMACEN));
        return creada;
    }

    @Override
    public Tienda obtenerPorId(UUID id) {
        return tiendaRepositoryPort.buscarPorId(id).orElseThrow(TiendaNoEncontradaException::new);
    }

    @Override
    public Page<Tienda> listar(Pageable pageable) {
        return tiendaRepositoryPort.listarTodas(pageable);
    }

    @Override
    public Tienda actualizar(UUID id, String nombre, String ubicacion) {
        Tienda tienda = obtenerPorId(id);
        if (!tienda.getNombre().equals(nombre) && tiendaRepositoryPort.existePorNombre(nombre)) {
            throw new TiendaYaExisteException();
        }
        return tiendaRepositoryPort.guardar(tienda.actualizar(nombre, ubicacion));
    }

    @Override
    public void desactivar(UUID id) {
        Tienda tienda = obtenerPorId(id);
        tiendaRepositoryPort.guardar(tienda.desactivar());
    }

    @Override
    public Tienda activar(UUID id) {
        Tienda tienda = obtenerPorId(id);
        return tiendaRepositoryPort.guardar(tienda.activar());
    }
}
