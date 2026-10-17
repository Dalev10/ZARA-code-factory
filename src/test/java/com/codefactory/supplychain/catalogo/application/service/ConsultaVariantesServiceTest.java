package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultaVariantesServiceTest {

    private final VarianteRepository varianteRepository = mock(VarianteRepository.class);
    private final ConsultaVariantesService servicio = new ConsultaVariantesService(varianteRepository);

    @Test
    void traduceLaVarianteAlResumenPublicadoConLaCategoriaDeSuTemplate() {
        Categoria categoria = Categoria.crear("Calzado");
        Template template = Template.crear("Zapato", "2026", "Proveedor", BigDecimal.TEN, categoria);
        Variante variante = new Variante("SKU-1", template, "42", "Negro");
        when(varianteRepository.findById(variante.getId())).thenReturn(Optional.of(variante));

        assertThat(servicio.buscar(variante.getId()))
                .contains(new VarianteResumen(variante.getId(), "SKU-1", categoria.getId()));
    }

    @Test
    void sinVarianteDevuelveVacio() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(servicio.buscar(id)).isEmpty();
    }
}
