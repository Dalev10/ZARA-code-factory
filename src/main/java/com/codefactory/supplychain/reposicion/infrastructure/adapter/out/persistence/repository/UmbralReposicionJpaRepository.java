package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.UmbralReposicionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de {@link UmbralReposicionEntity}.
 */
public interface UmbralReposicionJpaRepository extends JpaRepository<UmbralReposicionEntity, UUID> {

    Optional<UmbralReposicionEntity> findByVarianteId(UUID varianteId);

    Optional<UmbralReposicionEntity> findByCategoriaId(UUID categoriaId);

    boolean existsByVarianteId(UUID varianteId);

    boolean existsByCategoriaId(UUID categoriaId);
}
