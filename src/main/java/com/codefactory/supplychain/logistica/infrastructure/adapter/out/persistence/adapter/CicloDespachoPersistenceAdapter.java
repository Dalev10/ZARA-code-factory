package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.CicloDespachoEntity;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository.CicloDespachoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia de {@link CicloDespachoRepository}. */
@Repository
@RequiredArgsConstructor
public class CicloDespachoPersistenceAdapter implements CicloDespachoRepository {

    private final CicloDespachoJpaRepository jpaRepository;

    @Override
    public CicloDespacho guardar(CicloDespacho ciclo) {
        return aDominio(jpaRepository.save(new CicloDespachoEntity(ciclo.getId(), ciclo.getOrdenId(),
                ciclo.getNumeroCiclo(), ciclo.getCantidadSolicitada(), ciclo.getCantidadDespachada(),
                ciclo.getFechaDespacho(), ciclo.getResultado())));
    }

    @Override
    public Optional<CicloDespacho> buscarPorId(UUID cicloId) {
        return jpaRepository.findById(cicloId).map(CicloDespachoPersistenceAdapter::aDominio);
    }

    @Override
    public int ultimoNumeroCiclo(UUID ordenId) {
        return jpaRepository.ultimoNumeroCiclo(ordenId);
    }

    @Override
    public Page<CicloDespacho> listar(UUID ordenId, boolean sinViaje, Pageable pageable) {
        // El orden es fijo en la consulta nativa: un sort con nombres de propiedad no aplica a columnas.
        Pageable sinOrden = pageable.isPaged() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : Pageable.unpaged();
        return jpaRepository.buscar(ordenId, sinViaje, sinOrden).map(CicloDespachoPersistenceAdapter::aDominio);
    }

    private static CicloDespacho aDominio(CicloDespachoEntity e) {
        return CicloDespacho.reconstruir(e.getId(), e.getOrdenId(), e.getNumeroCiclo(), e.getCantidadSolicitada(),
                e.getCantidadDespachada(), e.getFechaDespacho());
    }
}
