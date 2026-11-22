package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Administración de los niveles de reposición al CD (HD-04, D-74).
 */
public interface NivelReposicionCdUseCase {

    /**
     * @param varianteId  variante del nivel, o {@code null} si es por categoría.
     * @param categoriaId categoría del nivel, o {@code null} si es por variante.
     */
    NivelReposicionCd crear(UUID varianteId, UUID categoriaId, int cantidadMinima, int cantidadObjetivo);

    NivelReposicionCd obtenerPorId(UUID id);

    Page<NivelReposicionCd> listar(Pageable pageable);

    NivelReposicionCd modificar(UUID id, int cantidadMinima, int cantidadObjetivo);

    void eliminar(UUID id);
}
