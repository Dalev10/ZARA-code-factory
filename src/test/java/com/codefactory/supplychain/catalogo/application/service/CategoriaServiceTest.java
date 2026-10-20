package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.exception.CategoriaReservadaException;
import com.codefactory.supplychain.catalogo.application.exception.CategoriaYaExisteException;
import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.domain.exception.CategoriaInvalidaException;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

class CategoriaServiceTest {

    private CategoriaRepository categoriaRepository;
    private CategoriaService servicio;

    @BeforeEach
    void setUp() {
        categoriaRepository = mock(CategoriaRepository.class);
        servicio = new CategoriaService(categoriaRepository);
    }

    @Test
    void creaUnaCategoriaValida() {
        when(categoriaRepository.existsByNombre("Calzado")).thenReturn(false);
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Categoria categoria = servicio.crear("Calzado");

        assertThat(categoria.getNombre()).isEqualTo("Calzado");
    }

    @Test
    void rechazaCrearConNombreVacio() {
        assertThatThrownBy(() -> servicio.crear("  ")).isInstanceOf(CategoriaInvalidaException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void rechazaCrearConNombreYaExistente() {
        when(categoriaRepository.existsByNombre("Calzado")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear("Calzado")).isInstanceOf(CategoriaYaExisteException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void obtenerPorIdRechazaUnaCategoriaInexistente() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id))
                .isInstanceOf(CatalogoRecursoNoEncontradoException.class);
    }

    @Test
    void listarDelegaAlRepositorio() {
        Categoria categoria = Categoria.crear("Calzado");
        Pageable pageable = Pageable.unpaged();
        when(categoriaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(categoria)));

        assertThat(servicio.listar(pageable)).containsExactly(categoria);
    }

    @Test
    void modificarCambiaElNombre() {
        Categoria categoria = Categoria.crear("Calzado");
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombre("Calzado Deportivo")).thenReturn(false);
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Categoria modificada = servicio.modificar(categoria.getId(), "Calzado Deportivo");

        assertThat(modificada.getNombre()).isEqualTo("Calzado Deportivo");
    }

    @Test
    void modificarPermiteConservarElMismoNombre() {
        Categoria categoria = Categoria.crear("Calzado");
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Categoria modificada = servicio.modificar(categoria.getId(), "Calzado");

        assertThat(modificada.getNombre()).isEqualTo("Calzado");
        verify(categoriaRepository, never()).existsByNombre(any());
    }

    @Test
    void modificarRechazaRenombrarAUnNombreDeOtraCategoria() {
        Categoria categoria = Categoria.crear("Calzado");
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombre("Ropa")).thenReturn(true);

        assertThatThrownBy(() -> servicio.modificar(categoria.getId(), "Ropa"))
                .isInstanceOf(CategoriaYaExisteException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminarRechazaUnaCategoriaInexistente() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(id)).isInstanceOf(CatalogoRecursoNoEncontradoException.class);
        verify(categoriaRepository, never()).deleteById(any());
    }

    /**
     * Escenario: SIN_CATEGORIA es un nombre reservado (D-52)
     *   No se puede crear otra categoría con ese nombre (sin distinguir mayúsculas)
     *   Ni renombrar SIN_CATEGORIA, ni renombrar otra hacia ese nombre, ni eliminarla
     */
    @Test
    void sinCategoriaEsUnNombreReservado() {
        Categoria reservada = Categoria.reconstruir(UUID.randomUUID(), Categoria.NOMBRE_RESERVADO);
        Categoria otra = Categoria.reconstruir(UUID.randomUUID(), "Camisas");
        when(categoriaRepository.findById(reservada.getId())).thenReturn(Optional.of(reservada));
        when(categoriaRepository.findById(otra.getId())).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> servicio.crear(" sin_categoria ")).isInstanceOf(CategoriaReservadaException.class);
        assertThatThrownBy(() -> servicio.modificar(reservada.getId(), "Camisas"))
                .isInstanceOf(CategoriaReservadaException.class);
        assertThatThrownBy(() -> servicio.modificar(otra.getId(), "SIN_CATEGORIA"))
                .isInstanceOf(CategoriaReservadaException.class);
        assertThatThrownBy(() -> servicio.eliminar(reservada.getId())).isInstanceOf(CategoriaReservadaException.class);
        verify(categoriaRepository, never()).save(any());
        verify(categoriaRepository, never()).deleteById(any());
    }
}
