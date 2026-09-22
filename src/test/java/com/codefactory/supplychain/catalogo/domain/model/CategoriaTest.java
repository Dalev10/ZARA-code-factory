package com.codefactory.supplychain.catalogo.domain.model;

import com.codefactory.supplychain.catalogo.domain.exception.CategoriaInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoriaTest {

    @Test
    void crearAsignaUnIdNuevo() {
        Categoria categoria = Categoria.crear("Calzado");

        assertThat(categoria.getId()).isNotNull();
        assertThat(categoria.getNombre()).isEqualTo("Calzado");
    }

    @Test
    void cambiarNombreDevuelveUnaNuevaInstanciaConElMismoId() {
        Categoria categoria = Categoria.crear("Calzado");

        Categoria renombrada = categoria.cambiarNombre("Calzado Deportivo");

        assertThat(renombrada.getId()).isEqualTo(categoria.getId());
        assertThat(renombrada.getNombre()).isEqualTo("Calzado Deportivo");
        assertThat(categoria.getNombre()).isEqualTo("Calzado");
    }

    @Test
    void dosInstanciasConElMismoIdSonIguales() {
        UUID id = UUID.randomUUID();

        Categoria a = Categoria.reconstruir(id, "Calzado");
        Categoria b = Categoria.reconstruir(id, "Otro nombre");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void crearConNombreVacioLanzaExcepcion() {
        assertThatThrownBy(() -> Categoria.crear("   ")).isInstanceOf(CategoriaInvalidaException.class);
    }

    @Test
    void crearConNombreNuloLanzaExcepcion() {
        assertThatThrownBy(() -> Categoria.crear(null)).isInstanceOf(CategoriaInvalidaException.class);
    }

    @Test
    void crearConNombreDemasiadoLargoLanzaExcepcion() {
        String nombreLargo = "a".repeat(151);

        assertThatThrownBy(() -> Categoria.crear(nombreLargo)).isInstanceOf(CategoriaInvalidaException.class);
    }
}
