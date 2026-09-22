package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.specification;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public class CentroDistribucionSpecification {

    private CentroDistribucionSpecification() {
    }

    public static Specification<CentroDistribucionEntity> conId(UUID id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<CentroDistribucionEntity> conNombre(String nombre) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    public static Specification<CentroDistribucionEntity> conUbicacion(String ubicacion) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("ubicacion")), "%" + ubicacion.toLowerCase() + "%");
    }
}
