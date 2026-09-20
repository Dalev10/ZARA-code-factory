package com.codefactory.supplychain.catalogo.application.port.in;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.domain.model.Variante;

import java.util.List;

/**
 * Puerto de entrada (Ports and Adapters) que define los casos de uso
 * disponibles para Variante / SKU (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Implementado por {@code VarianteService} y consumido por
 * {@code VarianteController} a través de DTOs.
 */
public interface VarianteUseCase {

    /**
     * Crea una nueva Variante asociada a un Template existente.
     * {@code talla} y {@code color} son opcionales (pueden ser
     * {@code null}).
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si el Template referenciado no existe.
     * @throws com.codefactory.supplychain.catalogo.application.exception.SkuDuplicadoException
     *         si ya existe una Variante con ese sku.
     */
    Variante crear(String sku, UUID templateId, String talla, String color);

    /**
     * Consulta una Variante por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Variante con ese id.
     */
    Variante obtenerPorId(UUID id);

    /**
     * Consulta una Variante por su sku.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Variante con ese sku.
     */
    Variante obtenerPorSku(String sku);

    /**
     * Lista todas las Variantes existentes.
     */
    List<Variante> listar();

    /**
     * Modifica el sku, el Template, la talla y/o el color de una
     * Variante existente. {@code nuevaTalla} y {@code nuevoColor} son
     * opcionales (pueden ser {@code null}).
     * <p>
     * El modelo de dominio {@code Variante} es inmutable (no expone
     * métodos de modificación); la actualización se realiza
     * reconstituyendo una nueva instancia con el mismo id y los nuevos
     * valores, y guardándola a través del puerto de salida — el mismo
     * patrón que ya usa la persistencia existente para reconstituir
     * entidades.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Variante con ese id, o si el Template
     *         referenciado no existe.
     * @throws com.codefactory.supplychain.catalogo.application.exception.SkuDuplicadoException
     *         si el nuevo sku ya pertenece a otra Variante distinta de
     *         esta.
     */
    Variante modificar(UUID id, String nuevoSku, UUID nuevoTemplateId, String nuevaTalla, String nuevoColor);

    /**
     * Elimina una Variante por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Variante con ese id.
     */
    void eliminar(UUID id);
}
