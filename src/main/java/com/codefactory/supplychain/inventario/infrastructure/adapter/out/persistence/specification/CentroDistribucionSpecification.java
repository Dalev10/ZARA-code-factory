package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.specification;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion.CentroDistribucionEntity;
import org.springframework.data.jpa.domain.Specification;

public class CentroDistribucionSpecification {

    public static Specification<CentroDistribucionEntity> conId(Integer id) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<CentroDistribucionEntity> conNombre(String nombre) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nombre")),
                        "%" + nombre.toLowerCase() + "%"
                );
    }

    public static Specification<CentroDistribucionEntity> conUbicacion(String ubicacion) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ubicacion")),
                        "%" + ubicacion.toLowerCase() + "%"
                );
    }
}