package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.centrodistribucion;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CentroDistribucionJpaRepository extends JpaRepository<CentroDistribucionEntity, Integer> {
    Optional<CentroDistribucionEntity> findByNombre(String nombre);
}
