package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.MovimientoInventarioMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.MovimientoInventarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
