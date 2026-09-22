package com.codefactory.supplychain.catalogo.domain.model;

import com.codefactory.supplychain.catalogo.domain.exception.TemplateInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateTest {

    private static Categoria categoria() {
        return Categoria.crear("Calzado");
    }

    @Test
    void crearAsignaUnIdNuevo() {
        Template template = Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria());

        assertThat(template.getId()).isNotNull();
        assertThat(template.getNombre()).isEqualTo("Zapatilla X");
        assertThat(template.getPrecioBase()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void actualizarInformacionDevuelveUnaNuevaInstanciaSinCambiarIdNiCategoria() {
        Categoria categoria = categoria();
        Template template = Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria);

        Template actualizado = template.actualizarInformacion("Zapatilla Y", "Invierno", "ProveedorY",
                BigDecimal.ONE);

        assertThat(actualizado.getId()).isEqualTo(template.getId());
        assertThat(actualizado.getNombre()).isEqualTo("Zapatilla Y");
        assertThat(actualizado.getTemporada()).isEqualTo("Invierno");
        assertThat(actualizado.getProveedor()).isEqualTo("ProveedorY");
        assertThat(actualizado.getPrecioBase()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(actualizado.getCategoria()).isEqualTo(categoria);
        assertThat(template.getNombre()).isEqualTo("Zapatilla X");
    }

    @Test
    void crearConNombreVacioLanzaExcepcion() {
        assertThatThrownBy(() -> Template.crear("  ", null, null, null, categoria()))
                .isInstanceOf(TemplateInvalidoException.class);
    }

    @Test
    void crearConPrecioBaseNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> Template.crear("Zapatilla X", null, null, BigDecimal.valueOf(-1), categoria()))
                .isInstanceOf(TemplateInvalidoException.class);
    }

    @Test
    void crearSinCategoriaLanzaExcepcion() {
        assertThatThrownBy(() -> Template.crear("Zapatilla X", null, null, null, null))
                .isInstanceOf(TemplateInvalidoException.class);
    }

    @Test
    void crearConPrecioBaseNuloEsValido() {
        Template template = Template.crear("Zapatilla X", null, null, null, categoria());

        assertThat(template.getPrecioBase()).isNull();
    }
}
