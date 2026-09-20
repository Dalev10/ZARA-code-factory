package com.codefactory.supplychain.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaTest {

    @Test
    void crearAsignaUnIdNuevo() {
        Categoria categoria = new Categoria("Calzado");

        assertThat(categoria.getId()).isNotNull();
        assertThat(categoria.getNombre()).isEqualTo("Calzado");
    }

    @Test
    void cambiarNombreModificaElNombreSinCambiarElId() {
        Categoria categoria = new Categoria("Calzado");
        UUID idOriginal = categoria.getId();

        categoria.cambiarNombre("Calzado Deportivo");

        assertThat(categoria.getId()).isEqualTo(idOriginal);
        assertThat(categoria.getNombre()).isEqualTo("Calzado Deportivo");
    }

    @Test
    void dosInstanciasConElMismoIdSonIguales() {
        UUID id = UUID.randomUUID();

        Categoria a = new Categoria(id, "Calzado");
        Categoria b = new Categoria(id, "Otro nombre");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
