package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;
public interface RegistrarBodegaTiendaUseCase {
    /**
     * @param tiendaId El identificador de la tienda a la que pertenece la bodega
     *                 tienda.
     * @return La bodega registrada.
     * @throws ...domain.exception.bodegatienda.BodegaTiendaYaExisteException
     * si una bodega ya existe para la tienda especificada.
    * @throws ...domain.exception.bodegatienda.TiendaAsociadaNoExisteException
     * si la tienda especificada no existe
     */
    BodegaTienda registrar(Long tiendaId);
}
