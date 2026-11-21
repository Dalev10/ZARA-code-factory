package com.codefactory.supplychain.logistica.application.port.out;

import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lectura de las paradas de viaje y de su ciclo de despacho que necesita la recepción (HU-49).
 * Solo lectura: la recepción no cambia el estado de la parada ni del ciclo.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface ParadasRecepcionPort {

    Optional<ParadaRecepcion> buscar(UUID paradaId);

    /** Paradas ENTREGADO sin recepción de las tiendas indicadas; vacío si no hay tiendas. */
    List<ParadaRecepcion> listarPorRecibir(Set<UUID> tiendas);

    /** Paradas ENTREGADO sin recepción de todas las tiendas. */
    List<ParadaRecepcion> listarTodasPorRecibir();
}
