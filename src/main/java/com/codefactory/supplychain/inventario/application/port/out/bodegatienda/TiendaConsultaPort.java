package com.codefactory.supplychain.inventario.application.port.out.bodegatienda;

import java.util.Optional;

/**
 * Puerto TEMPORAL de lectura de la entidad Tienda, perteneciente a otro
 * contexto funcional.
 */
public interface TiendaConsultaPort {

    boolean existe(Long tiendaId);

    Optional<TiendaInfo> buscarInfo(Long tiendaId);

    /**
     * DTO TEMPORAL de lectura compartido entre los features de Inventario y
     * Tienda.
     */
    record TiendaInfo(Long id, String nombre, String ubicacion) {
    }
}
