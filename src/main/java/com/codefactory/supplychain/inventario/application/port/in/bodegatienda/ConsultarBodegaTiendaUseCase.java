package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

public interface ConsultarBodegaTiendaUseCase {

    /**
     * Consultar una Bodega por su ID
     * 
     * @param id
     * @return
     * @throws ...domain.exception.bodegatienda.BodegaTiendaNoEncontradaException
     * si la bodega no existe.
     */
    BodegaTiendaConsulta consultarPorId(Long id);

    /**
     * Consultar una Bodega por el ID de la tienda a la que pertenece
     * 
     * @param tiendaId
     * @return
     * @throws ...domain.exception.bodegatienda.BodegaTiendaNoEncontradaException
     * si la bodega no existe.
     */
    BodegaTiendaConsulta consultarPorTiendaId(Long tiendaId);
}
