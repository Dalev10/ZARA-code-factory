package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.PrioridadCds;
import com.codefactory.supplychain.reposicion.application.port.out.CdsPorPrioridadPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * ACL hacia inventario.api: lista de CDs de la tienda por prioridad con {@link PrioridadCds}.
 *
 * Módulo: reposicion
 */
@Component
public class CdsPorPrioridadAdapter implements CdsPorPrioridadPort {

    private final PrioridadCds prioridadCds;

    public CdsPorPrioridadAdapter(PrioridadCds prioridadCds) {
        this.prioridadCds = prioridadCds;
    }

    @Override
    public List<UUID> cdsPorPrioridad(UUID tiendaId) {
        return prioridadCds.cdsPorPrioridad(tiendaId);
    }
}
