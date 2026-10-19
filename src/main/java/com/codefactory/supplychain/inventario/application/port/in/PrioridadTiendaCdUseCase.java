package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;

import java.util.List;
import java.util.UUID;

/**
 * HU-42: gestión de la lista de prioridad de CDs por tienda.
 * <p>
 * La lista se reemplaza completa con PUT: no hay PATCH por entrada porque
 * reordenar viola el UNIQUE en el paso intermedio. Hibernate puede borrar e insertar
 * en el mismo flush gracias al DEFERRABLE de V23.
 */
public interface PrioridadTiendaCdUseCase {

    /**
     * Reemplaza la lista completa de prioridades de la tienda. Si la lista está vacía,
     * borra todo. El índice en la lista (1-based) determina el {@code ordenPrioridad}.
     *
     * @param tiendaId id de la tienda.
     * @param cdIds    lista de ids de CDs en orden de prioridad (el primero = prioridad 1).
     * @return la lista persistida.
     */
    List<PrioridadTiendaCd> reemplazar(UUID tiendaId, List<UUID> cdIds);

    /**
     * @return lista de prioridades de la tienda, ordenada por {@code ordenPrioridad}.
     */
    List<PrioridadTiendaCd> listar(UUID tiendaId);

    /**
     * Bloquea la eliminación de un CD si tiene prioridades asignadas.
     * Lo invoca {@link CentroDistribucionUseCase#eliminarCentroDistribucion} antes de borrar.
     */
    void verificarQueNoEstaEnUso(UUID cdId);
}
