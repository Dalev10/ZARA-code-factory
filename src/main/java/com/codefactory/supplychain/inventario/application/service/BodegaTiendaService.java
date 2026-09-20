package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.in.BodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaConInventarioAsociadoException;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaInactivaException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * BodegaTienda no tiene tabla ni entidad propia — ES un {@link Nodo} de tipo
 * {@code BODEGA_TIENDA}. Este servicio aporta las reglas de negocio propias
 * (tienda activa, una bodega por tienda, no eliminar con inventario
 * asociado) sobre la persistencia genérica de {@link NodoRepositoryPort}.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Service
@RequiredArgsConstructor
public class BodegaTiendaService implements BodegaTiendaUseCase {

    private final NodoRepositoryPort nodoRepositoryPort;
    private final TiendaRepositoryPort tiendaRepositoryPort;
    private final InventarioPorNodoPort inventarioPorNodoPort;

    @Override
    @Transactional
    public Nodo registrar(UUID tiendaId) {
        validarTiendaActivaParaAsignacion(tiendaId);
        if (nodoRepositoryPort.existePorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)) {
            throw new BodegaTiendaYaExisteException();
        }
        return nodoRepositoryPort.guardar(Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA));
    }

    @Override
    public BodegaTiendaConsulta consultarPorId(UUID id) {
        return enriquecer(obtenerNodoBodegaTienda(id));
    }

    @Override
    public BodegaTiendaConsulta consultarPorTiendaId(UUID tiendaId) {
        Nodo nodo = nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)
                .orElseThrow(BodegaTiendaNoEncontradaException::new);
        return enriquecer(nodo);
    }

    @Override
    public List<Nodo> listarTodas() {
        return nodoRepositoryPort.listarPorTipo(TipoNodo.BODEGA_TIENDA);
    }

    @Override
    @Transactional
    public Nodo modificar(UUID id, UUID nuevaTiendaId) {
        Nodo actual = obtenerNodoBodegaTienda(id);
        if (!actual.getTiendaId().equals(nuevaTiendaId)) {
            validarTiendaActivaParaAsignacion(nuevaTiendaId);
            if (nodoRepositoryPort.existePorTiendaYTipo(nuevaTiendaId, TipoNodo.BODEGA_TIENDA)) {
                throw new BodegaTiendaYaExisteException();
            }
        }
        return nodoRepositoryPort.guardar(Nodo.reconstruir(id, TipoNodo.BODEGA_TIENDA, null, nuevaTiendaId));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        obtenerNodoBodegaTienda(id);
        if (!inventarioPorNodoPort.consultarPorNodo(id).isEmpty()) {
            throw new BodegaTiendaConInventarioAsociadoException();
        }
        nodoRepositoryPort.eliminar(id);
    }

    private void validarTiendaActivaParaAsignacion(UUID tiendaId) {
        Tienda tienda = tiendaRepositoryPort.buscarPorId(tiendaId).orElseThrow(TiendaNoEncontradaException::new);
        if (!tienda.estaActiva()) {
            throw new TiendaInactivaException();
        }
    }

    private Nodo obtenerNodoBodegaTienda(UUID id) {
        Nodo nodo = nodoRepositoryPort.buscarPorId(id).orElseThrow(BodegaTiendaNoEncontradaException::new);
        if (nodo.getTipo() != TipoNodo.BODEGA_TIENDA) {
            throw new BodegaTiendaNoEncontradaException();
        }
        return nodo;
    }

    private BodegaTiendaConsulta enriquecer(Nodo nodo) {
        Tienda tienda = tiendaRepositoryPort.buscarPorId(nodo.getTiendaId()).orElse(null);
        return new BodegaTiendaConsulta(nodo, tienda, inventarioPorNodoPort.consultarPorNodo(nodo.getId()));
    }
}
