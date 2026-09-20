package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
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

class TemplateServiceTest {

    private TemplateRepository templateRepository;
    private CategoriaRepository categoriaRepository;
    private TemplateService servicio;

    @BeforeEach
    void setUp() {
        templateRepository = mock(TemplateRepository.class);
        categoriaRepository = mock(CategoriaRepository.class);
        servicio = new TemplateService(templateRepository, categoriaRepository);
    }

    @Test
    void creaUnTemplateAsociadoAUnaCategoriaExistente() {
        Categoria categoria = Categoria.crear("Calzado");
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Template template = servicio.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria.getId());

        assertThat(template.getNombre()).isEqualTo("Zapatilla X");
        assertThat(template.getCategoria()).isEqualTo(categoria);
    }

    @Test
    void rechazaCrearConUnaCategoriaInexistente() {
        UUID categoriaId = UUID.randomUUID();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoriaId))
                .isInstanceOf(CatalogoRecursoNoEncontradoException.class);

        verify(templateRepository, never()).save(any());
    }

    @Test
    void obtenerPorIdRechazaUnTemplateInexistente() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id)).isInstanceOf(CatalogoRecursoNoEncontradoException.class);
    }

    @Test
    void listarDelegaAlRepositorio() {
        Template template = Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN,
                Categoria.crear("Calzado"));
        when(templateRepository.findAll()).thenReturn(List.of(template));

        assertThat(servicio.listar()).containsExactly(template);
    }

    @Test
    void modificarActualizaLaInformacionEditableSinTocarLaCategoria() {
        Categoria categoria = Categoria.crear("Calzado");
        Template template = Template.crear("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Template modificado = servicio.modificar(template.getId(), "Zapatilla Y", "Invierno", "ProveedorY",
                BigDecimal.ONE);

        assertThat(modificado.getNombre()).isEqualTo("Zapatilla Y");
        assertThat(modificado.getCategoria()).isEqualTo(categoria);
    }

    @Test
    void eliminarRechazaUnTemplateInexistente() {
        UUID id = UUID.randomUUID();
        when(templateRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(id)).isInstanceOf(CatalogoRecursoNoEncontradoException.class);
        verify(templateRepository, never()).deleteById(any());
    }
}
