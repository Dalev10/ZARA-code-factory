package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface CentroDistribucionJpaRepository
        extends JpaRepository<CentroDistribucionEntity, UUID>, JpaSpecificationExecutor<CentroDistribucionEntity> {

    List<CentroDistribucionEntity> findByNombre(String nombre);
}
