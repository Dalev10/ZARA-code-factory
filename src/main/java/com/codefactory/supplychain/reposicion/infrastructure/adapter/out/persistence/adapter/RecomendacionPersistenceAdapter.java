package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.application.port.out.RecomendacionRepository;
import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.RecomendacionPersistenceMapper;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository.RecomendacionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para {@link RecomendacionRepository}.
 *
 * Módulo: reposicion.
 */
@Component
public class RecomendacionPersistenceAdapter implements RecomendacionRepository {

    /** Valor de relleno para el IN cuando se listan todas las tiendas (el filtro no aplica). */
    private static final List<UUID> SIN_FILTRO = List.of(new UUID(0, 0));

    private final RecomendacionJpaRepository jpaRepository;
    private final RecomendacionPersistenceMapper mapper;

    public RecomendacionPersistenceAdapter(RecomendacionJpaRepository jpaRepository,
                                           RecomendacionPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Recomendacion guardar(Recomendacion recomendacion) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(recomendacion)));
    }

    @Override
    public Optional<Recomendacion> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existeBorrador(UUID tiendaId, UUID varianteId) {
        return jpaRepository.existsByTiendaIdAndVarianteIdAndEstado(tiendaId, varianteId,
                EstadoRecomendacion.BORRADOR);
    }

    @Override
    public boolean existeOrdenGeneradaSinCd(UUID tiendaId, UUID varianteId) {
        return jpaRepository.existeOrdenGeneradaSinCd(tiendaId, varianteId);
    }

    @Override
    public Page<Recomendacion> listar(EstadoRecomendacion estado, UUID tiendaId, UUID varianteId,
                                      Collection<UUID> tiendas, Pageable pageable) {
        // El orden lo fija la consulta; se ignora el sort del cliente.
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        boolean todas = tiendas == null;
        return jpaRepository.listar(estado, tiendaId, varianteId, todas, todas ? SIN_FILTRO : tiendas, pagina)
                .map(mapper::toDomain);
    }
}
