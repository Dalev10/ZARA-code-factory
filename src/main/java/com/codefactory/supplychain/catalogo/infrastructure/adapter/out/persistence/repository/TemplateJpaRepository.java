package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para {@link TemplateEntity}.
 * <p>
 * Los métodos requeridos (save, findById, findAll, existsById,
 * deleteById) ya los provee {@link JpaRepository}; no fue necesario
 * declarar métodos adicionales para esta etapa.
 */
public interface TemplateJpaRepository extends JpaRepository<TemplateEntity, Long> {
}
