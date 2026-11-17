package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.OrdenReabastecimientoPersistenceMapper;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository.OrdenReabastecimientoJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para {@link OrdenReabastecimientoRepository}.
 *
 * Módulo: reposicion.
 */
@Component
public class OrdenReabastecimientoPersistenceAdapter implements OrdenReabastecimientoRepository {

    /** Valor de relleno para el IN cuando se listan todas las tiendas (el filtro no aplica). */
    private static final List<UUID> SIN_FILTRO = List.of(new UUID(0, 0));

    private final OrdenReabastecimientoJpaRepository jpaRepository;
    private final OrdenReabastecimientoPersistenceMapper mapper;

    public OrdenReabastecimientoPersistenceAdapter(OrdenReabastecimientoJpaRepository jpaRepository,
                                                   OrdenReabastecimientoPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OrdenReabastecimiento guardar(OrdenReabastecimiento orden) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(orden)));
    }

    @Override
    public Optional<OrdenReabastecimiento> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<OrdenReabastecimiento> listar(EstadoOrden estado, UUID tiendaId, Collection<UUID> tiendas,
                                              Pageable pageable) {
        // El orden lo fija la consulta; se ignora el sort del cliente.
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        boolean todas = tiendas == null;
        return jpaRepository.listar(estado, tiendaId, todas, todas ? SIN_FILTRO : tiendas, pagina)
                .map(mapper::toDomain);
    }
}
