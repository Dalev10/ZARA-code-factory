package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.exception.SkuDuplicadoException;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VarianteServiceTest {

    private VarianteRepository varianteRepository;
    private TemplateRepository templateRepository;
    private VarianteService servicio;

    @BeforeEach
    void setUp() {
        varianteRepository = mock(VarianteRepository.class);
        templateRepository = mock(TemplateRepository.class);
        servicio = new VarianteService(varianteRepository, templateRepository);
    }

    private static Template template() {
        return Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, Categoria.crear("Calzado"));
    }

    @Test
    void creaUnaVarianteAsociadaAUnTemplateExistente() {
        Template template = template();
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(varianteRepository.existsBySku("SKU-001")).thenReturn(false);
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Variante variante = servicio.crear("SKU-001", template.getId(), "M", "Rojo");

        assertThat(variante.getSku()).isEqualTo("SKU-001");
        assertThat(variante.getTemplate()).isEqualTo(template);
    }

    @Test
    void rechazaCrearConUnTemplateInexistente() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear("SKU-001", templateId, "M", "Rojo"))
                .isInstanceOf(CatalogoRecursoNoEncontradoException.class);

        verify(varianteRepository, never()).save(any());
    }

    @Test
    void rechazaCrearConUnSkuYaExistente() {
        Template template = template();
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(varianteRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear("SKU-001", template.getId(), "M", "Rojo"))
                .isInstanceOf(SkuDuplicadoException.class);

        verify(varianteRepository, never()).save(any());
    }

    @Test
    void obtenerPorIdRechazaUnaVarianteInexistente() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id)).isInstanceOf(CatalogoRecursoNoEncontradoException.class);
    }

    @Test
    void obtenerPorSkuRechazaUnSkuInexistente() {
        when(varianteRepository.findBySku("NO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorSku("NO-EXISTE"))
                .isInstanceOf(CatalogoRecursoNoEncontradoException.class);
    }

    @Test
    void listarDelegaAlRepositorio() {
        Variante variante = new Variante("SKU-001", template(), "M", "Rojo");
        when(varianteRepository.findAll()).thenReturn(List.of(variante));

        assertThat(servicio.listar()).containsExactly(variante);
    }

    @Test
    void modificarPermiteConservarElMismoSku() {
        Template template = template();
        Variante variante = new Variante("SKU-001", template, "M", "Rojo");
        when(varianteRepository.findById(variante.getId())).thenReturn(Optional.of(variante));
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Variante modificada = servicio.modificar(variante.getId(), "SKU-001", template.getId(), "L", "Azul");

        assertThat(modificada.getTalla()).isEqualTo("L");
        assertThat(modificada.getColor()).isEqualTo("Azul");
        verify(varianteRepository, never()).existsBySku(any());
    }

    @Test
    void modificarRechazaUnNuevoSkuYaUsadoPorOtraVariante() {
        Template template = template();
        Variante variante = new Variante("SKU-001", template, "M", "Rojo");
        when(varianteRepository.findById(variante.getId())).thenReturn(Optional.of(variante));
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(varianteRepository.existsBySku("SKU-002")).thenReturn(true);

        assertThatThrownBy(() -> servicio.modificar(variante.getId(), "SKU-002", template.getId(), "M", "Rojo"))
                .isInstanceOf(SkuDuplicadoException.class);
    }

    @Test
    void eliminarRechazaUnaVarianteInexistente() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(id)).isInstanceOf(CatalogoRecursoNoEncontradoException.class);
        verify(varianteRepository, never()).deleteById(any());
    }
}
