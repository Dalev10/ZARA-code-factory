package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.in.BodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.out.ExistenciasRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * BodegaTienda no tiene tabla ni entidad propia: ES un {@link Nodo} de tipo
 * {@code BODEGA_TIENDA}, creado junto con su tienda (D-55). Este servicio solo la
 * consulta, enriquecida con la tienda y sus existencias.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BodegaTiendaService implements BodegaTiendaUseCase {

    private final NodoRepositoryPort nodoRepositoryPort;
    private final TiendaRepositoryPort tiendaRepositoryPort;
    private final ExistenciasRepositoryPort existenciasRepositoryPort;

    @Override
    public BodegaTiendaConsulta consultarPorId(UUID id) {
        Nodo nodo = nodoRepositoryPort.buscarPorId(id)
                .filter(n -> n.getTipo() == TipoNodo.BODEGA_TIENDA)
                .orElseThrow(BodegaTiendaNoEncontradaException::new);
        return enriquecer(nodo);
    }

    @Override
    public BodegaTiendaConsulta consultarPorTiendaId(UUID tiendaId) {
        Nodo nodo = nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)
                .orElseThrow(BodegaTiendaNoEncontradaException::new);
        return enriquecer(nodo);
    }

    @Override
    public Page<Nodo> listarTodas(Pageable pageable) {
        return nodoRepositoryPort.listarPorTipo(TipoNodo.BODEGA_TIENDA, pageable);
    }

    private BodegaTiendaConsulta enriquecer(Nodo nodo) {
        Tienda tienda = tiendaRepositoryPort.buscarPorId(nodo.getTiendaId()).orElse(null);
        return new BodegaTiendaConsulta(nodo, tienda, existenciasRepositoryPort.listarPorNodo(nodo.getId()));
    }
}
