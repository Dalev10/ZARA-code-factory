package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.UmbralYaExisteException;
import com.codefactory.supplychain.reposicion.application.port.out.UmbralReposicionRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort.VarianteConsultada;
import com.codefactory.supplychain.reposicion.domain.exception.UmbralInvalidoException;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
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

class UmbralReposicionServiceTest {

    private static final UUID VARIANTE = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();

    private UmbralReposicionRepository repositorio;
    private VarianteConsultaPort varianteConsulta;
    private UmbralReposicionService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(UmbralReposicionRepository.class);
        varianteConsulta = mock(VarianteConsultaPort.class);
        servicio = new UmbralReposicionService(repositorio, varianteConsulta);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creaUnUmbralPorVariante() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.of(new VarianteConsultada(VARIANTE, CATEGORIA)));
        when(repositorio.existePorVariante(VARIANTE)).thenReturn(false);

        UmbralReposicion umbral = servicio.crear(VARIANTE, null, 3, 6);

        assertThat(umbral.getVarianteId()).isEqualTo(VARIANTE);
        verify(repositorio).guardar(umbral);
    }

    @Test
    void creaUnUmbralPorCategoriaSinConsultarCatalogo() {
        when(repositorio.existePorCategoria(CATEGORIA)).thenReturn(false);

        UmbralReposicion umbral = servicio.crear(null, CATEGORIA, 2, 5);

        assertThat(umbral.getCategoriaId()).isEqualTo(CATEGORIA);
        verify(varianteConsulta, never()).buscar(any());
    }

    @Test
    void crearConVarianteInexistenteDevuelveNoEncontrado() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(VARIANTE, null, 3, 6))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void crearConVarianteQueYaTieneUmbralDevuelveDuplicado() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.of(new VarianteConsultada(VARIANTE, CATEGORIA)));
        when(repositorio.existePorVariante(VARIANTE)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(VARIANTE, null, 3, 6))
                .isInstanceOf(UmbralYaExisteException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void crearConCategoriaQueYaTieneUmbralDevuelveDuplicado() {
        when(repositorio.existePorCategoria(CATEGORIA)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(null, CATEGORIA, 3, 6))
                .isInstanceOf(UmbralYaExisteException.class);
    }

    @Test
    void crearConAlcanceOCantidadesInvalidasFallaAntesDeConsultarNada() {
        assertThatThrownBy(() -> servicio.crear(VARIANTE, CATEGORIA, 3, 6))
                .isInstanceOf(UmbralInvalidoException.class);
        assertThatThrownBy(() -> servicio.crear(VARIANTE, null, 3, 3))
                .isInstanceOf(UmbralInvalidoException.class);

        verify(varianteConsulta, never()).buscar(any());
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void obtenerPorIdInexistenteDevuelveNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class);
    }

    @Test
    void listarDelegaEnElRepositorio() {
        UmbralReposicion umbral = UmbralReposicion.crear(VARIANTE, null, 3, 6);
        when(repositorio.listar(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(umbral)));

        assertThat(servicio.listar(Pageable.unpaged())).containsExactly(umbral);
    }

    @Test
    void modificarCambiaLasCantidadesConservandoElAlcance() {
        UmbralReposicion existente = UmbralReposicion.crear(VARIANTE, null, 3, 6);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        UmbralReposicion modificado = servicio.modificar(existente.getId(), 8, 12);

        assertThat(modificado.getId()).isEqualTo(existente.getId());
        assertThat(modificado.getVarianteId()).isEqualTo(VARIANTE);
        assertThat(modificado.getCantidadMinima()).isEqualTo(8);
        assertThat(modificado.getCantidadObjetivo()).isEqualTo(12);
    }

    @Test
    void modificarInexistenteDevuelveNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(id, 8, 12))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class);
    }

    @Test
    void modificarConCantidadesInvalidasNoGuarda() {
        UmbralReposicion existente = UmbralReposicion.crear(VARIANTE, null, 3, 6);
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicio.modificar(existente.getId(), 6, 6))
                .isInstanceOf(UmbralInvalidoException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void eliminarBorraElUmbralExistente() {
        UUID id = UUID.randomUUID();
        when(repositorio.existePorId(id)).thenReturn(true);

        servicio.eliminar(id);

        verify(repositorio).eliminarPorId(id);
    }

    @Test
    void eliminarInexistenteDevuelveNoEncontradoSinBorrar() {
        UUID id = UUID.randomUUID();
        when(repositorio.existePorId(id)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(id))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class);
        verify(repositorio, never()).eliminarPorId(any());
    }
}
