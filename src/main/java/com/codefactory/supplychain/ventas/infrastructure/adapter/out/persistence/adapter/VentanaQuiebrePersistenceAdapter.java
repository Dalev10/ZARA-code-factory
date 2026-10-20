package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.VentanaQuiebreEntity;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository.VentanaQuiebreJpaRepository;
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
public class VentanaQuiebrePersistenceAdapter implements VentanaQuiebreRepositoryPort {

    private final VentanaQuiebreJpaRepository jpa;

    @Override
    public Optional<VentanaQuiebre> buscarPorId(UUID id) {
        return jpa.findById(id).map(VentanaQuiebrePersistenceAdapter::aDominio);
    }

    @Override
    public Optional<VentanaQuiebre> buscarAbierta(UUID tiendaId, UUID varianteId) {
        return jpa.findByTiendaIdAndVarianteIdAndFechaFinIsNull(tiendaId, varianteId)
                .map(VentanaQuiebrePersistenceAdapter::aDominio);
    }

    @Override
    public boolean abrirSiAusente(VentanaQuiebre v) {
        return jpa.abrirSiAusente(v.getId(), v.getVarianteId(), v.getTiendaId(), v.getFechaInicio()) == 1;
    }

    @Override
    public VentanaQuiebre guardar(VentanaQuiebre v) {
        return aDominio(jpa.save(VentanaQuiebreEntity.builder().id(v.getId()).tiendaId(v.getTiendaId())
                .varianteId(v.getVarianteId()).fechaInicio(v.getFechaInicio()).fechaFin(v.getFechaFin())
                .motivoCierre(v.getMotivoCierre()).build()));
    }

    @Override
    public List<VentanaQuiebre> listarPorTiendaYVariante(UUID tiendaId, UUID varianteId) {
        return jpa.findByTiendaIdAndVarianteIdOrderByFechaInicioAsc(tiendaId, varianteId).stream()
                .map(VentanaQuiebrePersistenceAdapter::aDominio).toList();
    }

    @Override
    public List<VentanaQuiebre> listarAbiertasPorTienda(UUID tiendaId) {
        return jpa.findByTiendaIdAndFechaFinIsNull(tiendaId).stream()
                .map(VentanaQuiebrePersistenceAdapter::aDominio).toList();
    }

    @Override
    public List<UUID> variantesConVentanas(UUID tiendaId) {
        return jpa.findVarianteIdsByTiendaId(tiendaId);
    }

    @Override
    public Page<VentanaQuiebre> buscar(Filtro filtro, Pageable pageable) {
        Specification<VentanaQuiebreEntity> spec = (root, query, cb) -> {
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
            if (filtro.abierta() != null) {
                condiciones.add(filtro.abierta() ? cb.isNull(root.get("fechaFin")) : cb.isNotNull(root.get("fechaFin")));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<Instant>get("fechaInicio"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<Instant>get("fechaInicio"), filtro.hasta()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return jpa.findAll(spec, pageable).map(VentanaQuiebrePersistenceAdapter::aDominio);
    }

    private static VentanaQuiebre aDominio(VentanaQuiebreEntity e) {
        return VentanaQuiebre.reconstituir(e.getId(), e.getTiendaId(), e.getVarianteId(), e.getFechaInicio(),
                e.getFechaFin(), e.getMotivoCierre());
    }
}
