package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Casos de uso de administración de umbrales de reposición interna (HU-38, D-57).
 */
public interface UmbralReposicionUseCase {

    /**
     * @param varianteId  variante del umbral, o {@code null} si es por categoría.
     * @param categoriaId categoría del umbral, o {@code null} si es por variante.
     */
    UmbralReposicion crear(UUID varianteId, UUID categoriaId, int cantidadMinima, int cantidadObjetivo);

    UmbralReposicion obtenerPorId(UUID id);

    Page<UmbralReposicion> listar(Pageable pageable);

    UmbralReposicion modificar(UUID id, int cantidadMinima, int cantidadObjetivo);

    void eliminar(UUID id);
}
