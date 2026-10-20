package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.MovimientoInventarioMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.MovimientoInventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.codefactory.supplychain.inventario.api.MovimientoKardex;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de persistencia del kardex.
 */
@Component
@RequiredArgsConstructor
public class MovimientoInventarioRepositoryAdapter
        implements MovimientoInventarioRepositoryPort {

    private final MovimientoInventarioJpaRepository repository;
    private final MovimientoInventarioMapper mapper;

    @Override
    public MovimientoInventario guardar(
            UUID nodoId,
            UUID varianteId,
            MovimientoInventario movimiento) {
        repository.save(mapper.toEntity(nodoId, varianteId, movimiento));
        return movimiento;
    }

    @Override
    public Page<MovimientoKardexConsulta> buscarKardex(
            UUID nodoId,
            TipoMovimientoInventario tipo,
            Instant desde,
            Instant hasta,
            Pageable pageable) {
        return repository.buscarKardex(nodoId, tipo, desde, hasta, pageable)
                .map(mapper::toConsulta);
    }

    @Override
    public List<MovimientoKardex> buscarPorNodoYVariante(UUID nodoId, UUID varianteId, Instant desde, Instant hasta) {
        return repository.findByNodoIdAndVarianteIdAndOcurridoEnBetweenOrderByOcurridoEnAscIdAsc(
                        nodoId, varianteId, desde, hasta).stream()
                .map(mapper::toKardex)
                .toList();
    }
}
