package com.codefactory.supplychain.catalogo.application.port.in;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.domain.model.Template;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de entrada (Ports and Adapters) que define los casos de uso
 * disponibles para Template (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Implementado por {@code TemplateService} y consumido por
 * {@code TemplateController} a través de DTOs.
 */
public interface TemplateUseCase {

    /**
     * Crea un nuevo Template asociado a una Categoria existente.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si la Categoria referenciada no existe.
     */
    Template crear(String nombre, String temporada, String proveedor,
                    BigDecimal precioBase, UUID categoriaId);

    /**
     * Consulta un Template por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe un Template con ese id.
     */
    Template obtenerPorId(UUID id);

    /**
     * Lista todos los Templates existentes.
     */
    List<Template> listar();

    /**
     * Modifica la información editable de un Template existente
     * (nombre, temporada, proveedor y precio base).
     * <p>
     * La Categoria del Template no se reasigna aquí: no hay un requisito
     * funcional que establezca que un Template pueda cambiar de Categoria.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe un Template con ese id.
     */
    Template modificar(UUID id, String nombre, String temporada,
                        String proveedor, BigDecimal precioBase);

    /**
     * Elimina un Template por su id.
     *
     * @throws com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException
     *         si no existe un Template con ese id.
     */
    void eliminar(UUID id);
}
