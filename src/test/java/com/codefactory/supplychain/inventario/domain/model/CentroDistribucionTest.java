package com.codefactory.supplychain.inventario.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CentroDistribucionTest {

    @Test
    void crearAsignaUnIdNuevo() {
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");

        assertThat(cd.getId()).isNotNull();
        assertThat(cd.getNombre()).isEqualTo("CD Principal");
        assertThat(cd.getUbicacion()).isEqualTo("Bogotá");
    }

    @Test
    void dosInstanciasConElMismoIdSonIguales() {
        UUID id = UUID.randomUUID();

        CentroDistribucion a = new CentroDistribucion(id, "CD A", "Bogotá");
        CentroDistribucion b = new CentroDistribucion(id, "CD A (renombrado)", "Cali");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void dosInstanciasConDistintoIdNoSonIguales() {
        CentroDistribucion a = CentroDistribucion.crear("CD A", "Bogotá");
        CentroDistribucion b = CentroDistribucion.crear("CD A", "Bogotá");

        assertThat(a).isNotEqualTo(b);
    }
}
