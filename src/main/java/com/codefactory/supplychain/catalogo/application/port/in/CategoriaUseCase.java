package com.codefactory.supplychain.catalogo.application.port.in;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Puerto de entrada (Ports and Adapters) que define los casos de uso
 * disponibles para Categoria (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Implementado por {@code CategoriaService} y consumido por
 * {@code CategoriaController} a través de DTOs.
 */
public interface CategoriaUseCase {

    /**
     * Crea una nueva Categoria.
     */
    Categoria crear(String nombre);

    /**
     * Consulta una Categoria por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    Categoria obtenerPorId(UUID id);

    /**
     * Lista las Categorias existentes, paginadas.
     */
    Page<Categoria> listar(Pageable pageable);

    /**
     * Modifica el nombre de una Categoria existente.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    Categoria modificar(UUID id, String nuevoNombre);

    /**
     * Elimina una Categoria por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe una Categoria con ese id.
     */
    void eliminar(UUID id);
}
