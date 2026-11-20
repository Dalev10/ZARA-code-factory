package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.application.port.out.NivelReposicionCdRepository;
import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.NivelReposicionCdEntity;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository.NivelReposicionCdJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de {@link NivelReposicionCdRepository}.
 */
@Repository
public class NivelReposicionCdPersistenceAdapter implements NivelReposicionCdRepository {

    private final NivelReposicionCdJpaRepository jpaRepository;

    public NivelReposicionCdPersistenceAdapter(NivelReposicionCdJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NivelReposicionCd guardar(NivelReposicionCd nivel) {
        return aDominio(jpaRepository.save(new NivelReposicionCdEntity(nivel.getId(), nivel.getVarianteId(),
                nivel.getCategoriaId(), nivel.getCantidadMinima(), nivel.getCantidadObjetivo())));
    }

    @Override
    public Optional<NivelReposicionCd> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(NivelReposicionCdPersistenceAdapter::aDominio);
    }

    @Override
    public Optional<NivelReposicionCd> buscarPorVariante(UUID varianteId) {
        return jpaRepository.findByVarianteId(varianteId).map(NivelReposicionCdPersistenceAdapter::aDominio);
    }

    @Override
    public Optional<NivelReposicionCd> buscarPorCategoria(UUID categoriaId) {
        return jpaRepository.findByCategoriaId(categoriaId).map(NivelReposicionCdPersistenceAdapter::aDominio);
    }

    @Override
    public Page<NivelReposicionCd> listar(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(NivelReposicionCdPersistenceAdapter::aDominio);
    }

    @Override
    public boolean existePorId(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existePorVariante(UUID varianteId) {
        return jpaRepository.existsByVarianteId(varianteId);
    }

    @Override
    public boolean existePorCategoria(UUID categoriaId) {
        return jpaRepository.existsByCategoriaId(categoriaId);
    }

    @Override
    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static NivelReposicionCd aDominio(NivelReposicionCdEntity entity) {
        return NivelReposicionCd.reconstruir(entity.getId(), entity.getVarianteId(), entity.getCategoriaId(),
                entity.getCantidadMinima(), entity.getCantidadObjetivo());
    }
}
