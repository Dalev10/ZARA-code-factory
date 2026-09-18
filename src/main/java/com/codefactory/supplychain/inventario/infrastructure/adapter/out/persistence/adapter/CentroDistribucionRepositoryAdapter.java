package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.CentroDistribucionJpaRepository;
import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.CentroDistribucionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CentroDistribucionRepositoryAdapter
        implements CentroDistribucionRepository {

    private final CentroDistribucionJpaRepository centroDistribucionJpaRepository;
    private final CentroDistribucionMapper centroDistribucionMapper;

    public CentroDistribucionRepositoryAdapter(
            CentroDistribucionJpaRepository centroDistribucionJpaRepository,
            CentroDistribucionMapper centroDistribucionMapper) {
        this.centroDistribucionJpaRepository = centroDistribucionJpaRepository;
        this.centroDistribucionMapper = centroDistribucionMapper;
    }

    @Override
    public CentroDistribucion findById(int id) {
        CentroDistribucionEntity entity =
                centroDistribucionJpaRepository.findById(id).orElse(null);

        return entity != null ? centroDistribucionMapper.toDomain(entity) : null;
    }

    @Override
    public CentroDistribucion findByNombre(String nombre) {
        CentroDistribucionEntity entity =
                centroDistribucionJpaRepository.findByNombre(nombre).orElse(null);

        return entity != null ? centroDistribucionMapper.toDomain(entity) : null;
    }

    @Override
    public CentroDistribucion crearCentroDistribucion(String nombre, String ubicacion) {
        CentroDistribucionEntity entity = new CentroDistribucionEntity();
        entity.setNombre(nombre);
        entity.setUbicacion(ubicacion);
        CentroDistribucionEntity savedEntity =
                centroDistribucionJpaRepository.save(entity);

        return centroDistribucionMapper.toDomain(savedEntity);

    }

    @Override
    public CentroDistribucion actualizarCentroDistribucion(
            CentroDistribucion centroDistribucion) {

        CentroDistribucionEntity entity =
                centroDistribucionJpaRepository
                        .findById(centroDistribucion.getId())
                        .orElse(null);

        if (entity == null) {
            return null;
        }

        entity.setNombre(centroDistribucion.getNombre());
        entity.setUbicacion(centroDistribucion.getUbicacion());
        CentroDistribucionEntity updatedEntity =
                centroDistribucionJpaRepository.save(entity);

        return centroDistribucionMapper.toDomain(updatedEntity);
    }

    @Override
    public void eliminarCentroDistribucion(int id) {
        centroDistribucionJpaRepository.deleteById(id);
    }


}
