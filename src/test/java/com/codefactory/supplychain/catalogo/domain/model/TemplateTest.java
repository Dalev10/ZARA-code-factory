package com.codefactory.supplychain.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTest {

    private static Categoria categoria() {
        return new Categoria("Calzado");
    }

    @Test
    void crearAsignaUnIdNuevo() {
        Template template = new Template("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria());

        assertThat(template.getId()).isNotNull();
        assertThat(template.getNombre()).isEqualTo("Zapatilla X");
        assertThat(template.getPrecioBase()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void actualizarInformacionModificaLosCamposEditablesSinCambiarIdNiCategoria() {
        Categoria categoria = categoria();
        Template template = new Template("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria);

        template.actualizarInformacion("Zapatilla Y", "Invierno", "ProveedorY", BigDecimal.ONE);

        assertThat(template.getNombre()).isEqualTo("Zapatilla Y");
        assertThat(template.getTemporada()).isEqualTo("Invierno");
        assertThat(template.getProveedor()).isEqualTo("ProveedorY");
        assertThat(template.getPrecioBase()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(template.getCategoria()).isEqualTo(categoria);
    }
}
