package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

public interface RegistrarBodegaTiendaUseCase {
    /**
     * @param tiendaId El identificador de la tienda a la que pertenece la bodega
     *                 tienda.
     * @return El identificador de la bodega tienda registrada.
     * @throws ...domain.exception.bodegatienda.BodegaTiendaYaExisteException
     * si una bodega ya existe para la tienda especificada.
     * @throws ...domain.exception.tienda.TiendaNoEncontradaException
     * si la tienda especificada no existe.
     */
    Long registrar(Long tiendaId);
}
