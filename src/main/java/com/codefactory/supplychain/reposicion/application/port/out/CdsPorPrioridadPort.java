package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * CDs que abastecen a una tienda, del de mayor prioridad al de menor.
 *
 * Módulo: reposicion.
 */
public interface CdsPorPrioridadPort {

    /** Lista vacía si la tienda no tiene CDs configurados. */
    List<UUID> cdsPorPrioridad(UUID tiendaId);
}
