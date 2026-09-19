package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter.centrodistribucion;

import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.centrodistribucion.CentroDistribucionJpaRepository;
import com.codefactory.supplychain.inventario.application.port.out.centrodistribucion.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.centrodistribucion.CentroDistribucionMapper;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.specification.CentroDistribucionSpecification;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository.NodoJpaRepository;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.NodoEntity;
import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CentroDistribucionRepositoryAdapter
        implements CentroDistribucionRepository {

    private final CentroDistribucionJpaRepository centroDistribucionJpaRepository;
    private final CentroDistribucionMapper centroDistribucionMapper;
    private final NodoJpaRepository nodoJpaRepository;

    public CentroDistribucionRepositoryAdapter(
            CentroDistribucionJpaRepository centroDistribucionJpaRepository,
            CentroDistribucionMapper centroDistribucionMapper,
            NodoJpaRepository nodoJpaRepository) {
        this.centroDistribucionJpaRepository = centroDistribucionJpaRepository;
        this.centroDistribucionMapper = centroDistribucionMapper;
        this.nodoJpaRepository = nodoJpaRepository;
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

        NodoEntity nodo = new NodoEntity();
        nodo.setTipo("CD");
        nodo.setCdId(savedEntity.getId().longValue());
        nodo.setTiendaId(null);

        nodoJpaRepository.save(nodo);

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

    @Override
    public CentroDistribucionConNodo buscarPorIdConNodo(int id) {

        CentroDistribucionEntity centroDistribucionEntity =
                centroDistribucionJpaRepository.findById(id).orElse(null);

        if (centroDistribucionEntity == null) {
            return null;
        }

        NodoEntity nodoEntity =
                nodoJpaRepository.findByCdId((long) id).orElse(null);

        if (nodoEntity == null) {
            return new CentroDistribucionConNodo(
                    centroDistribucionEntity.getId(),
                    centroDistribucionEntity.getNombre(),
                    centroDistribucionEntity.getUbicacion(),
                    null,
                    null,
                    null,
                    null
            );
        }

        return new CentroDistribucionConNodo(
                centroDistribucionEntity.getId(),
                centroDistribucionEntity.getNombre(),
                centroDistribucionEntity.getUbicacion(),
                nodoEntity.getId(),
                nodoEntity.getTipo(),
                nodoEntity.getCdId(),
                nodoEntity.getTiendaId()
        );
    }

}
