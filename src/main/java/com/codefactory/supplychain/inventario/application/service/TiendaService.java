package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ConsultaTiendas;
import com.codefactory.supplychain.inventario.api.TiendaResumen;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crea una Tienda y sus nodos satélite {@code ALMACEN} y
 * {@code BODEGA_TIENDA} en la misma transacción.
 *
 * También implementa ConsultaTiendas, la consulta de solo lectura que inventario publica
 * para otros módulos en su paquete api (ver ARQUITECTURA.md).
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Service
@RequiredArgsConstructor
public class TiendaService implements TiendaUseCase, ConsultaTiendas {

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
        nodoRepositoryPort.guardar(Nodo.crearParaTienda(creada.getId(), TipoNodo.BODEGA_TIENDA));
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

    @Override
    public Optional<TiendaResumen> buscar(UUID tiendaId) {
        return tiendaRepositoryPort.buscarPorId(tiendaId).map(TiendaService::resumir);
    }

    @Override
    public List<TiendaResumen> buscarTodas(Collection<UUID> tiendaIds) {
        return tiendaRepositoryPort.buscarPorIds(tiendaIds).stream().map(TiendaService::resumir).toList();
    }

    private static TiendaResumen resumir(Tienda tienda) {
        return new TiendaResumen(tienda.getId(), tienda.getNombre(), tienda.estaActiva());
    }
}
