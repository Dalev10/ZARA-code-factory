package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.CategoriaPersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository.CategoriaJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link CategoriaRepository}
 * apoyándose en Spring Data JPA.
 */
@Repository
public class CategoriaPersistenceAdapter implements CategoriaRepository {

    private final CategoriaJpaRepository categoriaJpaRepository;
    private final CategoriaPersistenceMapper categoriaPersistenceMapper;

    public CategoriaPersistenceAdapter(CategoriaJpaRepository categoriaJpaRepository,
                                        CategoriaPersistenceMapper categoriaPersistenceMapper) {
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.categoriaPersistenceMapper = categoriaPersistenceMapper;
    }

    @Override
    public Categoria save(Categoria categoria) {
        CategoriaEntity entity = categoriaPersistenceMapper.toEntity(categoria);
        CategoriaEntity saved = categoriaJpaRepository.save(entity);
        return categoriaPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Categoria> findById(UUID id) {
        return categoriaJpaRepository.findById(id)
                .map(categoriaPersistenceMapper::toDomain);
    }

    @Override
    public List<Categoria> findAll() {
        return categoriaJpaRepository.findAll().stream()
                .map(categoriaPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return categoriaJpaRepository.existsById(id);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return categoriaJpaRepository.existsByNombre(nombre);
    }

    @Override
    public void deleteById(UUID id) {
        categoriaJpaRepository.deleteById(id);
    }
}
