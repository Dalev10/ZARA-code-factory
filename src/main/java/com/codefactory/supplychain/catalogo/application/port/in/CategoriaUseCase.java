package com.codefactory.supplychain.catalogo.application.port.in;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;

import java.util.List;

/**
 * Puerto de entrada (Ports and Adapters) que define los casos de uso
 * disponibles para Categoria (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Será implementado por {@code CategoriaService} y, en una etapa
 * posterior, consumido por un Controller REST a través de DTOs. En esta
 * etapa no existe todavía ningún Controller.
 */
public interface CategoriaUseCase {

    /**
     * Crea una nueva Categoria.
     */
    Categoria crear(String nombre);

    /**
     * Consulta una Categoria por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.RecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    Categoria obtenerPorId(Long id);

    /**
     * Lista todas las Categorias existentes.
     */
    List<Categoria> listar();

    /**
     * Modifica el nombre de una Categoria existente.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.RecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    Categoria modificar(Long id, String nuevoNombre);

    /**
     * Elimina una Categoria por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.RecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    void eliminar(Long id);
}
