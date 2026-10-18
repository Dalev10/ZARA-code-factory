package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.PrioridadTiendaCdRepository;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.PrioridadTiendaCdMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.PrioridadTiendaCdJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PrioridadTiendaCdPersistenceAdapter implements PrioridadTiendaCdRepository {

    private final PrioridadTiendaCdJpaRepository jpa;
    private final PrioridadTiendaCdMapper mapper;

    public PrioridadTiendaCdPersistenceAdapter(PrioridadTiendaCdJpaRepository jpa,
                                               PrioridadTiendaCdMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<PrioridadTiendaCd> listarPorTienda(UUID tiendaId) {
        return jpa.findByTiendaIdOrderByOrdenPrioridad(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void guardarTodas(List<PrioridadTiendaCd> prioridades) {
        jpa.saveAll(prioridades.stream().map(mapper::toEntity).toList());
    }

    @Override
    public void eliminarPorTienda(UUID tiendaId) {
        jpa.deleteByTiendaId(tiendaId);
    }

    @Override
    public boolean existePorCd(UUID cdId) {
        return jpa.existsByCdId(cdId);
    }
}
