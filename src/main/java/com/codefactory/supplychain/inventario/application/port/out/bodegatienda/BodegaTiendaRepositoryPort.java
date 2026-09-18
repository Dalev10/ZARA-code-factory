package com.codefactory.supplychain.inventario.application.port.out.bodegatienda;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de {@code BodegaTienda}.
 * Define las operaciones CRUD y de consulta que la capa de aplicación espera
 * de la capa de infraestructura.
 */
public interface BodegaTiendaRepositoryPort {

    /**
     * Guarda (crea o actualiza) una bodega tienda.
     * 
     * @param bodegaTienda La bodega tienda a guardar.
     * @return La bodega tienda guardada con su identificador asignado si fue
     *         creada.
     */
    BodegaTienda guardar(BodegaTienda bodegaTienda);

    /**
     * Busca una bodega tienda por su identificador.
     * 
     * @param id El identificador de la bodega tienda.
     * @return Un {@code Optional} que contiene la bodega tienda si existe, o vacío
     *         si no se encuentra.
     */
    Optional<BodegaTienda> buscarPorId(Long id);

    /**
     * Busca una bodega tienda por el identificador de la tienda a la que pertenece.
     * 
     * @param tiendaId El identificador de la tienda.
     * @return Un {@code Optional} que contiene la bodega tienda si existe, o vacío
     *         si no se encuentra.
     */
    Optional<BodegaTienda> buscarPorTiendaId(Long tiendaId);

    /**
     * Verifica si existe una bodega tienda por su identificador.
     * 
     * @param id El identificador de la bodega tienda.
     * @return {@code true} si existe la bodega tienda, {@code false} en caso
     *         contrario.
     */
    boolean existePorId(Long id);

    /**
     * Verifica si existe una bodega tienda asociada a una tienda por su
     * identificador.
     * 
     * @param tiendaId El identificador de la tienda.
     * @return {@code true} si existe una bodega tienda asociada a la tienda,
     *         {@code false} en caso contrario.
     */
    boolean existePorTiendaId(Long tiendaId);

    /**
     * Elimina una bodega tienda por su identificador.
     * 
     * @param id El identificador de la bodega tienda a eliminar.
     * @throws ...domain.exception.bodegatienda.BodegaTiendaNoEncontradaException
     * si la bodega tienda no existe.
     */
    void eliminar(Long id);
}
