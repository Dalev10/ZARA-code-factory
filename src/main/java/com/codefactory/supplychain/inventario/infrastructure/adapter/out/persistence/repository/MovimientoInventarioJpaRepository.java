package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.MovimientoInventarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio JPA del kardex de Inventario.
 */
public interface MovimientoInventarioJpaRepository
        extends JpaRepository<MovimientoInventarioEntity, UUID> {
}
