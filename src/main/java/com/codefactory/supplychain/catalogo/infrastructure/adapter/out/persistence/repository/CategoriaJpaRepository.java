package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link CategoriaEntity}.
 */
public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, UUID> {
}
