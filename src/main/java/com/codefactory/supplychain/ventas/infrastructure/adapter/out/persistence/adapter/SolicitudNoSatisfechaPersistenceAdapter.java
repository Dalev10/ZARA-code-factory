package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.ventas.application.port.out.SolicitudNoSatisfechaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.SolicitudNoSatisfechaEntity;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository.SolicitudNoSatisfechaJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SolicitudNoSatisfechaPersistenceAdapter implements SolicitudNoSatisfechaRepositoryPort {

    private final SolicitudNoSatisfechaJpaRepository jpa;

    @Override
    public Optional<SolicitudNoSatisfecha> buscarPorId(UUID id) {
        return jpa.findById(id).map(SolicitudNoSatisfechaPersistenceAdapter::aDominio);
    }

    @Override
    public Optional<SolicitudNoSatisfecha> buscarPorClave(String claveIdempotencia) {
        return jpa.findByClaveIdempotencia(claveIdempotencia).map(SolicitudNoSatisfechaPersistenceAdapter::aDominio);
    }

    @Override
    public boolean insertarSiAusente(SolicitudNoSatisfecha s) {
        return jpa.insertarSiAusente(s.getId(), s.getVarianteId(), s.getTiendaId(), s.getCantidad(),
                s.getStockSistema(), s.getRegistradaEn(), s.getRegistradoPor(), s.getClaveIdempotencia()) == 1;
    }

    @Override
    public SolicitudNoSatisfecha guardar(SolicitudNoSatisfecha s) {
        return aDominio(jpa.save(aEntidad(s)));
    }

    @Override
    public Page<SolicitudNoSatisfecha> buscar(Filtro filtro, Pageable pageable) {
        Specification<SolicitudNoSatisfechaEntity> spec = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.tiendasPermitidas() != null) {
                condiciones.add(filtro.tiendasPermitidas().isEmpty()
                        ? cb.disjunction() : root.get("tiendaId").in(filtro.tiendasPermitidas()));
            }
            if (filtro.tiendaId() != null) {
                condiciones.add(cb.equal(root.get("tiendaId"), filtro.tiendaId()));
            }
            if (filtro.varianteId() != null) {
                condiciones.add(cb.equal(root.get("varianteId"), filtro.varianteId()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<Instant>get("registradaEn"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<Instant>get("registradaEn"), filtro.hasta()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return jpa.findAll(spec, pageable).map(SolicitudNoSatisfechaPersistenceAdapter::aDominio);
    }

    private static SolicitudNoSatisfecha aDominio(SolicitudNoSatisfechaEntity e) {
        return SolicitudNoSatisfecha.reconstituir(e.getId(), e.getTiendaId(), e.getVarianteId(), e.getCantidad(),
                e.getStockSistema(), e.getRegistradaEn(), e.getRegistradoPor(), e.getClaveIdempotencia(),
                e.getEstado(), e.getAnuladaPor(), e.getAnuladaEn(), e.getMotivoAnulacion());
    }

    private static SolicitudNoSatisfechaEntity aEntidad(SolicitudNoSatisfecha s) {
        return SolicitudNoSatisfechaEntity.builder().id(s.getId()).tiendaId(s.getTiendaId())
                .varianteId(s.getVarianteId()).cantidad(s.getCantidad()).stockSistema(s.getStockSistema())
                .registradaEn(s.getRegistradaEn()).registradoPor(s.getRegistradoPor())
                .claveIdempotencia(s.getClaveIdempotencia()).estado(s.getEstado())
                .anuladaPor(s.getAnuladaPor()).anuladaEn(s.getAnuladaEn())
                .motivoAnulacion(s.getMotivoAnulacion()).build();
    }
}
