package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.PrioridadCds;
import com.codefactory.supplychain.inventario.application.port.out.PrioridadTiendaCdRepository;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementa {@link PrioridadCds} sobre la configuración Tienda-CD de HU-42.
 * El repositorio ya devuelve las entradas ordenadas por {@code orden_prioridad}.
 *
 * Módulo: inventario.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PrioridadCdsAdapter implements PrioridadCds {

    private final PrioridadTiendaCdRepository prioridades;

    @Override
    public List<UUID> cdsPorPrioridad(UUID tiendaId) {
        Objects.requireNonNull(tiendaId, "tiendaId es obligatorio");
        return prioridades.listarPorTienda(tiendaId).stream()
                .map(PrioridadTiendaCd::getCdId)
                .toList();
    }
}
