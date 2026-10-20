package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.application.port.out.UmbralReposicionRepository;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.UmbralReposicionEntity;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.UmbralReposicionPersistenceMapper;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository.UmbralReposicionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de {@link UmbralReposicionRepository}.
 */
@Repository
public class UmbralReposicionPersistenceAdapter implements UmbralReposicionRepository {

    private final UmbralReposicionJpaRepository jpaRepository;
    private final UmbralReposicionPersistenceMapper mapper;

    public UmbralReposicionPersistenceAdapter(UmbralReposicionJpaRepository jpaRepository,
                                              UmbralReposicionPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UmbralReposicion guardar(UmbralReposicion umbral) {
        UmbralReposicionEntity guardada = jpaRepository.save(mapper.toEntity(umbral));
        return mapper.toDomain(guardada);
    }

    @Override
    public Optional<UmbralReposicion> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UmbralReposicion> buscarPorVariante(UUID varianteId) {
        return jpaRepository.findByVarianteId(varianteId).map(mapper::toDomain);
    }

    @Override
    public Optional<UmbralReposicion> buscarPorCategoria(UUID categoriaId) {
        return jpaRepository.findByCategoriaId(categoriaId).map(mapper::toDomain);
    }

    @Override
    public Page<UmbralReposicion> listar(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
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
}
