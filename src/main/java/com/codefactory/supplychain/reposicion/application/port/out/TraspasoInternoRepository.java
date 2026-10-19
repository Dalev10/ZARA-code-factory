package com.codefactory.supplychain.reposicion.application.port.out;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de los traspasos internos.
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface TraspasoInternoRepository {

    TraspasoInterno guardar(TraspasoInterno traspaso);

    Optional<TraspasoInterno> buscarPorId(UUID id);

    List<TraspasoInterno> listarPorTienda(UUID tiendaId);
}
