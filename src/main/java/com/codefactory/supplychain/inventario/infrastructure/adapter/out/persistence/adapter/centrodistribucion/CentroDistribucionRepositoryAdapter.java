package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.centrodistribucion.CentroDistribucionJpaRepository;
import com.codefactory.supplychain.inventario.application.port.out.centrodistribucion.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.centrodistribucion.CentroDistribucionMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.specification.CentroDistribucionSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.util.List;

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
        List<CentroDistribucionEntity> entities =
                centroDistribucionJpaRepository.findByNombre(nombre);

        return !entities.isEmpty() ? centroDistribucionMapper.toDomain(entities.get(0)) : null;
    }

    @Override
    public List<CentroDistribucion> buscar(
            Integer id,
            String nombre,
            String ubicacion) {

        Specification<CentroDistribucionEntity> specification = null;

        if (id != null) {
            specification = CentroDistribucionSpecification.conId(id);
        }

        if (nombre != null && !nombre.isBlank()) {
            Specification<CentroDistribucionEntity> nombreSpec =
                    CentroDistribucionSpecification.conNombre(nombre);

            specification = specification == null
                    ? nombreSpec
                    : specification.and(nombreSpec);
        }

        if (ubicacion != null && !ubicacion.isBlank()) {
            Specification<CentroDistribucionEntity> ubicacionSpec =
                    CentroDistribucionSpecification.conUbicacion(ubicacion);

            specification = specification == null
                    ? ubicacionSpec
                    : specification.and(ubicacionSpec);
        }

        List<CentroDistribucionEntity> entities =
                centroDistribucionJpaRepository.findAll(specification);

        return entities.stream()
                .map(centroDistribucionMapper::toDomain)
                .toList();
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
