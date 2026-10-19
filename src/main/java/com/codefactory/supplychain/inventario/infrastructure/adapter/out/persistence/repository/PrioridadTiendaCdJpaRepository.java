package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.PrioridadTiendaCdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PrioridadTiendaCdJpaRepository extends JpaRepository<PrioridadTiendaCdEntity, UUID> {

    List<PrioridadTiendaCdEntity> findByTiendaIdOrderByOrdenPrioridad(UUID tiendaId);

    @Modifying
    @Query("DELETE FROM PrioridadTiendaCdEntity p WHERE p.tiendaId = :tiendaId")
    void deleteByTiendaId(UUID tiendaId);

    boolean existsByCdId(UUID cdId);
}
