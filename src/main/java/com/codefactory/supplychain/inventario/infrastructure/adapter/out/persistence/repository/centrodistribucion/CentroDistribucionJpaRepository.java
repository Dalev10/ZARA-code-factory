package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.centrodistribucion;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CentroDistribucionJpaRepository extends JpaRepository<CentroDistribucionEntity, Integer>, JpaSpecificationExecutor<CentroDistribucionEntity> {
    
    List<CentroDistribucionEntity> findByNombre(String nombre);
    
}
