package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.VarianteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link VarianteEntity}.
 * <p>
 * save, findById, findAll, existsById y deleteById ya los provee
 * {@link JpaRepository}. Se añaden únicamente los métodos derivados
 * necesarios para buscar/comprobar por SKU, tal como se solicitó.
 */
public interface VarianteJpaRepository extends JpaRepository<VarianteEntity, Long> {

    Optional<VarianteEntity> findBySku(String sku);

    boolean existsBySku(String sku);
}
