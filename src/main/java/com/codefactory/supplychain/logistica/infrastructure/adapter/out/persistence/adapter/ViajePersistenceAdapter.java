package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.logistica.application.port.out.ViajeRepository;
import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.domain.model.ParadaViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.ViajeEntity;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.ViajeParadaEntity;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository.ViajeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia de {@link ViajeRepository}. */
@Repository
@RequiredArgsConstructor
public class ViajePersistenceAdapter implements ViajeRepository {

    private final ViajeJpaRepository jpaRepository;

    @Override
    public Viaje guardar(Viaje viaje) {
        return aDominio(jpaRepository.save(aEntidad(viaje)));
    }

    @Override
    public Optional<Viaje> buscarPorId(UUID viajeId) {
        return jpaRepository.findById(viajeId).map(ViajePersistenceAdapter::aDominio);
    }

    @Override
    public Optional<Viaje> buscarParaActualizar(UUID viajeId) {
        return jpaRepository.buscarConIncrementoDeVersion(viajeId).map(ViajePersistenceAdapter::aDominio);
    }

    @Override
    public Page<Viaje> listar(EstadoViaje estado, Pageable pageable) {
        return (estado == null ? jpaRepository.findAll(pageable) : jpaRepository.findByEstado(estado, pageable))
                .map(ViajePersistenceAdapter::aDominio);
    }

    @Override
    public boolean cicloEnViaje(UUID cicloDespachoId) {
        return jpaRepository.existeParadaDelCiclo(cicloDespachoId);
    }

    private static ViajeEntity aEntidad(Viaje viaje) {
        var paradas = new ArrayList<ViajeParadaEntity>();
        for (ParadaViaje p : viaje.getParadas()) {
            paradas.add(new ViajeParadaEntity(p.getId(), p.getCicloDespachoId(), p.getTiendaId(), p.getOrdenParada(),
                    p.getEstado(), p.getFechaSalidaCd(), p.getFechaLlegada()));
        }
        return new ViajeEntity(viaje.getId(), viaje.getCdId(), viaje.getVehiculo(), viaje.getEstado(),
                viaje.getFechaCreacion(), viaje.getFechaSalida(), viaje.getFechaCierre(), viaje.getVersion(), paradas);
    }

    private static Viaje aDominio(ViajeEntity e) {
        return Viaje.reconstruir(e.getId(), e.getCdId(), e.getVehiculo(), e.getFechaCreacion(), e.getVersion(),
                e.getParadas().stream()
                        .map(p -> ParadaViaje.reconstruir(p.getId(), p.getCicloDespachoId(), p.getTiendaId(),
                                p.getOrdenParada(), p.getEstado(), p.getFechaSalidaCd(), p.getFechaLlegada()))
                        .toList(),
                e.getEstado(), e.getFechaSalida(), e.getFechaCierre());
    }
}
