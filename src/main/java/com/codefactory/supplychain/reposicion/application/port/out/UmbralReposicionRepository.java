package com.codefactory.supplychain.reposicion.application.port.out;

import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de persistencia de {@link UmbralReposicion}.
 */
public interface UmbralReposicionRepository {

    UmbralReposicion guardar(UmbralReposicion umbral);

    Optional<UmbralReposicion> buscarPorId(UUID id);

    Optional<UmbralReposicion> buscarPorVariante(UUID varianteId);

    Optional<UmbralReposicion> buscarPorCategoria(UUID categoriaId);

    Page<UmbralReposicion> listar(Pageable pageable);

    boolean existePorId(UUID id);

    boolean existePorVariante(UUID varianteId);

    boolean existePorCategoria(UUID categoriaId);

    void eliminarPorId(UUID id);
}
