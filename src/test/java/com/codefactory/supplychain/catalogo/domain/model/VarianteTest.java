package com.codefactory.supplychain.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VarianteTest {

    private static Template template() {
        return Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, Categoria.crear("Calzado"));
    }

    @Test
    void crearAsignaUnIdNuevo() {
        Variante variante = new Variante("SKU-001", template(), "M", "Rojo");

        assertThat(variante.getId()).isNotNull();
        assertThat(variante.getSku()).isEqualTo("SKU-001");
        assertThat(variante.getTalla()).isEqualTo("M");
        assertThat(variante.getColor()).isEqualTo("Rojo");
    }

    @Test
    void permiteTallaYColorNulos() {
        Variante variante = new Variante("SKU-002", template(), null, null);

        assertThat(variante.getTalla()).isNull();
        assertThat(variante.getColor()).isNull();
    }
}
