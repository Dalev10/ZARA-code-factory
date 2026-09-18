package com.codefactory.supplychain.inventario.application.port.out.bodegatienda;

public interface TiendaExistePort {
    /**
     * @param tiendaId El identificador de la tienda a verificar.
     * @return true si la tienda existe, false en caso contrario.
     */
    boolean existeTienda(Long tiendaId);
}
