package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.List;
import java.util.UUID;

/**
 * Consulta de traspasos internos (HU-49), limitada a las tiendas del usuario.
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface ConsultarTraspasosUseCase {

    /**
     * Traspasos de la tienda, el más reciente primero.
     *
     * @throws com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException
     *         si el usuario no tiene asignada la tienda
     */
    List<TraspasoInterno> listarPorTienda(UsuarioActual usuario, UUID tiendaId);

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si no existe o es de una tienda ajena (no se revela su existencia)
     */
    TraspasoInterno obtener(UsuarioActual usuario, UUID id);
}
