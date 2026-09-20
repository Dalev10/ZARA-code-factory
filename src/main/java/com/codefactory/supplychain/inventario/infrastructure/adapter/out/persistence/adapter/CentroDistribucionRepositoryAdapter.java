package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.CentroDistribucionMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.CentroDistribucionJpaRepository;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.specification.CentroDistribucionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Component
@RequiredArgsConstructor
public class CentroDistribucionRepositoryAdapter implements CentroDistribucionRepository {

    private final CentroDistribucionJpaRepository jpaRepository;
    private final CentroDistribucionMapper mapper;

    @Override
    public CentroDistribucion guardar(CentroDistribucion centroDistribucion) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(centroDistribucion)));
    }

    @Override
    public Optional<CentroDistribucion> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CentroDistribucion> buscarPorNombre(String nombre) {
        return jpaRepository.findByNombre(nombre).stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public List<CentroDistribucion> buscar(UUID id, String nombre, String ubicacion) {
        Specification<CentroDistribucionEntity> specification = null;

        if (id != null) {
            specification = CentroDistribucionSpecification.conId(id);
        }
        if (nombre != null && !nombre.isBlank()) {
            Specification<CentroDistribucionEntity> nombreSpec = CentroDistribucionSpecification.conNombre(nombre);
            specification = specification == null ? nombreSpec : specification.and(nombreSpec);
        }
        if (ubicacion != null && !ubicacion.isBlank()) {
            Specification<CentroDistribucionEntity> ubicacionSpec =
                    CentroDistribucionSpecification.conUbicacion(ubicacion);
            specification = specification == null ? ubicacionSpec : specification.and(ubicacionSpec);
        }

        return jpaRepository.findAll(specification).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
