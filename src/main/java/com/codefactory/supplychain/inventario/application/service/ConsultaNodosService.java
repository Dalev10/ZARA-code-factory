package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementa el contrato público {@link ConsultaNodos} para que otros módulos
 * puedan resolver nodos de una tienda sin acceder al dominio interno de inventario.
 *
 * Módulo: inventario.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaNodosService implements ConsultaNodos {

    private final NodoRepositoryPort nodoRepository;

    public ConsultaNodosService(NodoRepositoryPort nodoRepository) {
        this.nodoRepository = nodoRepository;
    }

    @Override
    public Optional<UUID> buscarNodoAlmacen(UUID tiendaId) {
        return nodoRepository.buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN)
                .map(nodo -> nodo.getId());
    }

    @Override
    public Optional<UUID> buscarNodoBodega(UUID tiendaId) {
        return nodoRepository.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)
                .map(nodo -> nodo.getId());
    }

    @Override
    public Optional<UUID> buscarTiendaPorNodoAlmacen(UUID nodoId) {
        return nodoRepository.buscarPorId(nodoId)
                .filter(nodo -> nodo.getTipo() == TipoNodo.ALMACEN)
                .map(nodo -> nodo.getTiendaId());
    }
} 