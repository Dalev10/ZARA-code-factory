package com.codefactory.supplychain.reposicion.application.port.out;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de las órdenes de reabastecimiento y sus intentos de CD.
 *
 * Módulo: reposicion.
 */
public interface OrdenReabastecimientoRepository {

    OrdenReabastecimiento guardar(OrdenReabastecimiento orden);

    Optional<OrdenReabastecimiento> buscarPorId(UUID id);

    /**
     * Órdenes filtradas, la más reciente primero. Los filtros nulos no filtran.
     *
     * @param tiendas tiendas permitidas; {@code null} = todas
     */
    Page<OrdenReabastecimiento> listar(EstadoOrden estado, UUID tiendaId, Collection<UUID> tiendas,
                                       Pageable pageable);
}
