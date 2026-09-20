package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

public interface EliminarBodegaTiendaUseCase {
    /**
     * 
     * @param id El identificador de la bodega tienda a eliminar.
     * @throws ...domain.exception.bodegatienda.BodegaTiendaNoEncontradaException
     * si la bodega tienda no existe.
    */
    void eliminar(Long id);
}
