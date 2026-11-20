package com.codefactory.supplychain.reposicion.application.port.out;

import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de persistencia de {@link NivelReposicionCd}.
 */
public interface NivelReposicionCdRepository {

    NivelReposicionCd guardar(NivelReposicionCd nivel);

    Optional<NivelReposicionCd> buscarPorId(UUID id);

    Optional<NivelReposicionCd> buscarPorVariante(UUID varianteId);

    Optional<NivelReposicionCd> buscarPorCategoria(UUID categoriaId);

    Page<NivelReposicionCd> listar(Pageable pageable);

    boolean existePorId(UUID id);

    boolean existePorVariante(UUID varianteId);

    boolean existePorCategoria(UUID categoriaId);

    void eliminarPorId(UUID id);
}
