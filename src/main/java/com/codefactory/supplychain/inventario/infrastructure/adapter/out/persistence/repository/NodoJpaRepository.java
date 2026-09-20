package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.NodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NodoJpaRepository extends JpaRepository<NodoEntity, Long> {

    Optional<NodoEntity> findByCdId(Long cdId);

}