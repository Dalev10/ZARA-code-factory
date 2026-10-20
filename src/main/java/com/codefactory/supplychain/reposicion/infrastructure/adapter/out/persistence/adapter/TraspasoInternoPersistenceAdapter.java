package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.TraspasoInternoPersistenceMapper;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository.TraspasoInternoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para {@link TraspasoInternoRepository}.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class TraspasoInternoPersistenceAdapter implements TraspasoInternoRepository {

    private final TraspasoInternoJpaRepository jpaRepository;
    private final TraspasoInternoPersistenceMapper mapper;

    public TraspasoInternoPersistenceAdapter(TraspasoInternoJpaRepository jpaRepository,
                                              TraspasoInternoPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TraspasoInterno guardar(TraspasoInterno traspaso) {
        var entity = jpaRepository.save(mapper.toEntity(traspaso));
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<TraspasoInterno> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TraspasoInterno> listarPorTienda(UUID tiendaId) {
        return jpaRepository.findByTiendaIdOrderByTimestampTraspasoDesc(tiendaId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
