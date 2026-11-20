package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.NivelReposicionCdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de {@link NivelReposicionCdEntity}.
 */
public interface NivelReposicionCdJpaRepository extends JpaRepository<NivelReposicionCdEntity, UUID> {

    Optional<NivelReposicionCdEntity> findByVarianteId(UUID varianteId);

    Optional<NivelReposicionCdEntity> findByCategoriaId(UUID categoriaId);

    boolean existsByVarianteId(UUID varianteId);

    boolean existsByCategoriaId(UUID categoriaId);
}
