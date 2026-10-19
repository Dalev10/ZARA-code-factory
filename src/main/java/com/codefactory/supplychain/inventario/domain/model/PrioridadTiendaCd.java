package com.codefactory.supplychain.inventario.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Una entrada en la lista de prioridad de CDs para una tienda.
 * El orden determina qué CD se intenta abastecer primero; cada tienda tiene su
 * propio rango de órdenes, que deben ser positivos y sin huecos.
 *
 * Módulo: inventario (HU-42).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class PrioridadTiendaCd {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tiendaId;
    private final UUID cdId;
    private final int ordenPrioridad;

    private PrioridadTiendaCd(UUID id, UUID tiendaId, UUID cdId, int ordenPrioridad) {
        if (ordenPrioridad <= 0) {
            throw new IllegalArgumentException("El orden de prioridad debe ser mayor que cero");
        }
        this.id = id;
        this.tiendaId = tiendaId;
        this.cdId = cdId;
        this.ordenPrioridad = ordenPrioridad;
    }

    public static PrioridadTiendaCd crear(UUID tiendaId, UUID cdId, int ordenPrioridad) {
        return new PrioridadTiendaCd(UUID.randomUUID(), tiendaId, cdId, ordenPrioridad);
    }

    public static PrioridadTiendaCd reconstruir(UUID id, UUID tiendaId, UUID cdId, int ordenPrioridad) {
        return new PrioridadTiendaCd(id, tiendaId, cdId, ordenPrioridad);
    }
}
