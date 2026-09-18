package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

public interface ModificarBodegaTiendaUseCase {
    /**
     * @param id
     * @param bodegaTienda
     * @return
     * @throws ...domain.exception.bodegatienda.BodegaTiendaNoEncontradaException
     * si la bodega no existe.
     * @throws ...domain.exception.bodegatienda.BodegaTiendaInvalidaException     
     * si la bodega no es válida.
     */
    BodegaTienda modificar(Long id, BodegaTienda bodegaTienda);
}
