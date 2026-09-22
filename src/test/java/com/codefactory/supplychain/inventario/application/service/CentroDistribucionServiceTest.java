package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.CentroDistribucionConNodo;
import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionDuplicadoException;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class CentroDistribucionServiceTest {

    private CentroDistribucionRepository centroDistribucionRepository;
    private NodoRepositoryPort nodoRepositoryPort;
    private CentroDistribucionService servicio;

    @BeforeEach
    void setUp() {
        centroDistribucionRepository = mock(CentroDistribucionRepository.class);
        nodoRepositoryPort = mock(NodoRepositoryPort.class);
        servicio = new CentroDistribucionService(centroDistribucionRepository, nodoRepositoryPort);
    }

    @Test
    void crearGuardaElCdYAprovisionaSuNodo() {
        when(centroDistribucionRepository.buscarPorNombre("CD Principal")).thenReturn(Optional.empty());
        when(centroDistribucionRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        CentroDistribucion cd = servicio.crearCentroDistribucion("CD Principal", "Bogotá");

        ArgumentCaptor<Nodo> captor = ArgumentCaptor.forClass(Nodo.class);
        verify(nodoRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNodo.CD);
        assertThat(captor.getValue().getCdId()).isEqualTo(cd.getId());
    }

    @Test
    void rechazaCrearUnCdConNombreDuplicado() {
        when(centroDistribucionRepository.buscarPorNombre("CD Principal"))
                .thenReturn(Optional.of(CentroDistribucion.crear("CD Principal", "Bogotá")));

        assertThatThrownBy(() -> servicio.crearCentroDistribucion("CD Principal", "Bogotá"))
                .isInstanceOf(CentroDistribucionDuplicadoException.class);

        verify(nodoRepositoryPort, never()).guardar(any());
    }

    @Test
    void actualizarPermiteConservarElMismoNombre() {
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");
        when(centroDistribucionRepository.buscarPorId(cd.getId())).thenReturn(Optional.of(cd));
        when(centroDistribucionRepository.buscarPorNombre("CD Principal")).thenReturn(Optional.of(cd));
        when(centroDistribucionRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        CentroDistribucion actualizado = servicio.actualizarCentroDistribucion(cd.getId(), "CD Principal", "Cali");

        assertThat(actualizado.getUbicacion()).isEqualTo("Cali");
    }

    @Test
    void actualizarRechazaRenombrarAUnNombreDeOtroCd() {
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");
        CentroDistribucion otro = CentroDistribucion.crear("CD Secundario", "Cali");
        when(centroDistribucionRepository.buscarPorId(cd.getId())).thenReturn(Optional.of(cd));
        when(centroDistribucionRepository.buscarPorNombre("CD Secundario")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> servicio.actualizarCentroDistribucion(cd.getId(), "CD Secundario", "Bogotá"))
                .isInstanceOf(CentroDistribucionDuplicadoException.class);
    }

    @Test
    void eliminarBorraElNodoAsociadoAntesQueElCd() {
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");
        Nodo nodo = Nodo.crearParaCd(cd.getId());
        when(centroDistribucionRepository.buscarPorId(cd.getId())).thenReturn(Optional.of(cd));
        when(nodoRepositoryPort.buscarPorCdId(cd.getId())).thenReturn(Optional.of(nodo));

        servicio.eliminarCentroDistribucion(cd.getId());

        verify(nodoRepositoryPort).eliminar(nodo.getId());
        verify(centroDistribucionRepository).eliminar(cd.getId());
    }

    @Test
    void eliminarUnCdInexistenteLanzaExcepcionSinTocarNingunPuerto() {
        UUID id = UUID.randomUUID();
        when(centroDistribucionRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminarCentroDistribucion(id))
                .isInstanceOf(CentroDistribucionNoEncontradoException.class);

        verify(nodoRepositoryPort, never()).eliminar(any());
        verify(centroDistribucionRepository, never()).eliminar(any());
    }

    @Test
    void obtenerPorIdRechazaUnCdInexistente() {
        UUID id = UUID.randomUUID();
        when(centroDistribucionRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerCentroDistribucionPorId(id))
                .isInstanceOf(CentroDistribucionNoEncontradoException.class);
    }

    @Test
    void buscarSinNingunParametroFuncionaComoGetAllYDelegaAlRepositorio() {
        Pageable pageable = Pageable.unpaged();
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");
        when(centroDistribucionRepository.buscar(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(cd)));

        assertThat(servicio.buscarCentrosDistribucion(null, null, null, pageable)).containsExactly(cd);
    }

    @Test
    void buscarConAlMenosUnParametroDelegaAlRepositorio() {
        Pageable pageable = Pageable.unpaged();
        when(centroDistribucionRepository.buscar(null, "Principal", null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(servicio.buscarCentrosDistribucion(null, "Principal", null, pageable)).isEmpty();
    }

    @Test
    void buscarCentroDistribucionConNodoCombinaAmbosPuertos() {
        CentroDistribucion cd = CentroDistribucion.crear("CD Principal", "Bogotá");
        Nodo nodo = Nodo.crearParaCd(cd.getId());
        when(centroDistribucionRepository.buscarPorId(cd.getId())).thenReturn(Optional.of(cd));
        when(nodoRepositoryPort.buscarPorCdId(cd.getId())).thenReturn(Optional.of(nodo));

        CentroDistribucionConNodo resultado = servicio.buscarCentroDistribucionConNodo(cd.getId());

        assertThat(resultado.centroDistribucion()).isEqualTo(cd);
        assertThat(resultado.nodo()).isEqualTo(nodo);
    }
}
