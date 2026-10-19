package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaVariantesServiceTest {

    private final VarianteRepository varianteRepository = mock(VarianteRepository.class);
    private final ConsultaVariantesService servicio = new ConsultaVariantesService(varianteRepository);

    private final Categoria categoria = Categoria.crear("Calzado");
    private final Template template = Template.crear("Zapato", "2026", "Proveedor", BigDecimal.TEN, categoria);

    @Test
    void traduceLaVarianteAlResumenPublicadoConLaCategoriaDeSuTemplate() {
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

    @Test
    void buscarPorSkuDevuelveUnResumenDeLaVariante() {
        Variante variante = new Variante("SKU-001", template, "M", "Rojo");
        when(varianteRepository.findBySku("SKU-001")).thenReturn(Optional.of(variante));

        assertThat(servicio.buscarPorSku("SKU-001"))
                .contains(new VarianteResumen(variante.getId(), "SKU-001", categoria.getId()));
    }

    @Test
    void buscarPorSkuDevuelveVacioCuandoNoExiste() {
        when(varianteRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        assertThat(servicio.buscarPorSku("NO-EXISTE")).isEmpty();
    }

    @Test
    void buscarPorSkusDevuelveSoloLasVariantesExistentes() {
        Variante variante = new Variante("SKU-001", template, null, null);
        when(varianteRepository.findBySkuIn(Set.of("SKU-001", "SKU-FALTA"))).thenReturn(List.of(variante));

        List<VarianteResumen> resultado = servicio.buscarPorSkus(Set.of("SKU-001", "SKU-FALTA"));

        assertThat(resultado).containsExactly(new VarianteResumen(variante.getId(), "SKU-001", categoria.getId()));
    }

    @Test
    void buscarPorSkusSinSkusNoConsultaElRepositorio() {
        assertThat(servicio.buscarPorSkus(Set.of())).isEmpty();
        verify(varianteRepository, never()).findBySkuIn(any());
    }
}
