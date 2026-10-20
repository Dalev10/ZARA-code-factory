package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.VentaEntity;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.mapper.VentaMapper;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository.VentaJpaRepository;
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
public class VentaRepositoryAdapter implements VentaRepositoryPort {

    private final VentaJpaRepository jpaRepository;

    @Override
    public List<Venta> guardarTodas(List<Venta> ventas) {
        return jpaRepository.saveAllAndFlush(ventas.stream().map(VentaMapper::toEntity).toList())
                .stream().map(VentaMapper::toDomain).toList();
    }

    @Override
    public void bloquearTransaccion(UUID transaccionId) {
        jpaRepository.bloquearTransaccion(transaccionId);
    }

    @Override
    public List<Venta> buscarPorTransaccion(UUID transaccionId) {
        return jpaRepository.findByTransaccionIdOrderByVarianteId(transaccionId)
                .stream().map(VentaMapper::toDomain).toList();
    }

    @Override
    public List<Venta> buscarPorTransaccionParaActualizar(UUID transaccionId) {
        return jpaRepository.findByTransaccionIdParaActualizar(transaccionId)
                .stream().map(VentaMapper::toDomain).toList();
    }

    @Override
    public Optional<Venta> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(VentaMapper::toDomain);
    }

    @Override
    public Optional<Venta> buscarPorIdParaActualizar(UUID id) {
        return jpaRepository.buscarPorIdParaActualizar(id).map(VentaMapper::toDomain);
    }

    @Override
    public Venta guardar(Venta venta) {
        return VentaMapper.toDomain(jpaRepository.saveAndFlush(VentaMapper.toEntity(venta)));
    }

    @Override
    public Page<Venta> buscar(Filtro filtro, Pageable pageable) {
        Specification<VentaEntity> spec = (root, query, cb) -> {
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
            if (filtro.transaccionId() != null) {
                condiciones.add(cb.equal(root.get("transaccionId"), filtro.transaccionId()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<Instant>get("timestampVenta"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<Instant>get("timestampVenta"), filtro.hasta()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return jpaRepository.findAll(spec, pageable).map(VentaMapper::toDomain);
    }
}
