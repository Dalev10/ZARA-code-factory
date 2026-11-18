package com.codefactory.supplychain.inventario.api;

import java.util.List;
import java.util.UUID;

/**
 * Lista de CDs que abastecen a una tienda, según la prioridad configurada
 * (HU-42). Consumidor: reposicion (HU-43 sugiere el primero; HU-45 recorre la
 * lista en el fallback de CD).
 */
public interface PrioridadCds {

    /**
     * Ids de los CDs de la tienda, del de mayor prioridad al de menor. Lista
     * vacía si la tienda no tiene CDs configurados.
     */
    List<UUID> cdsPorPrioridad(UUID tiendaId);
}
