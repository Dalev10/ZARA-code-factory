package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.logistica.application.port.out.RecepcionRepository;
import com.codefactory.supplychain.logistica.domain.model.Recepcion;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.mapper.RecepcionPersistenceMapper;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository.RecepcionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de {@link RecepcionRepository}.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Repository
@RequiredArgsConstructor
public class RecepcionPersistenceAdapter implements RecepcionRepository {

    private final RecepcionJpaRepository jpaRepository;

    @Override
    public Optional<Recepcion> buscarPorParada(UUID viajeParadaId) {
        return jpaRepository.findByViajeParadaId(viajeParadaId).map(RecepcionPersistenceMapper::aDominio);
    }

    @Override
    public Recepcion guardar(Recepcion recepcion) {
        return RecepcionPersistenceMapper.aDominio(
                jpaRepository.saveAndFlush(RecepcionPersistenceMapper.aEntidad(recepcion)));
    }
}
