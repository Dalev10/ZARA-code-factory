package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.TiendaDesactivada;
import com.codefactory.supplychain.inventario.api.TiendaResumen;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.model.EstadoTienda;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TiendaServiceTest {

    private TiendaRepositoryPort tiendaRepositoryPort;
    private NodoRepositoryPort nodoRepositoryPort;
    private ApplicationEventPublisher eventos;
    private TiendaService servicio;

    @BeforeEach
    void setUp() {
        tiendaRepositoryPort = mock(TiendaRepositoryPort.class);
        nodoRepositoryPort = mock(NodoRepositoryPort.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new TiendaService(tiendaRepositoryPort, nodoRepositoryPort, eventos);
    }

    @Test
    void crearGuardaLaTiendaYAprovisionaSusNodosAlmacenYBodega() {
        when(tiendaRepositoryPort.existePorNombre("Tienda Centro")).thenReturn(false);
        when(tiendaRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Tienda tienda = servicio.crear("Tienda Centro", "Bogotá");

        assertThat(tienda.getNombre()).isEqualTo("Tienda Centro");

        ArgumentCaptor<Nodo> captor = ArgumentCaptor.forClass(Nodo.class);
        verify(nodoRepositoryPort, org.mockito.Mockito.times(2)).guardar(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Nodo::getTipo)
                .containsExactlyInAnyOrder(TipoNodo.ALMACEN, TipoNodo.BODEGA_TIENDA);
        assertThat(captor.getAllValues())
                .extracting(Nodo::getTiendaId)
                .containsOnly(tienda.getId());
    }

    @Test
    void rechazaCrearUnaTiendaConNombreDuplicado() {
        when(tiendaRepositoryPort.existePorNombre("Tienda Centro")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear("Tienda Centro", "Bogotá"))
                .isInstanceOf(TiendaYaExisteException.class);

        verify(tiendaRepositoryPort, never()).guardar(any());
        verify(nodoRepositoryPort, never()).guardar(any());
    }

    @Test
    void obtenerPorIdRechazaUnaTiendaInexistente() {
        UUID id = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id)).isInstanceOf(TiendaNoEncontradaException.class);
    }

    @Test
    void listarDelegaAlPuerto() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");
        Pageable pageable = Pageable.unpaged();
        when(tiendaRepositoryPort.listarTodas(pageable)).thenReturn(new PageImpl<>(List.of(tienda)));

        assertThat(servicio.listar(pageable)).containsExactly(tienda);
    }

    @Test
    void actualizarPermiteConservarElMismoNombre() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");
        when(tiendaRepositoryPort.buscarPorId(tienda.getId())).thenReturn(Optional.of(tienda));
        when(tiendaRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.actualizar(tienda.getId(), "Tienda Centro", "Nueva ubicación");

        verify(tiendaRepositoryPort, never()).existePorNombre(any());
    }

    @Test
    void actualizarRechazaRenombrarAUnNombreYaUsadoPorOtraTienda() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");
        when(tiendaRepositoryPort.buscarPorId(tienda.getId())).thenReturn(Optional.of(tienda));
        when(tiendaRepositoryPort.existePorNombre("Tienda Norte")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(tienda.getId(), "Tienda Norte", "Bogotá"))
                .isInstanceOf(TiendaYaExisteException.class);
    }

    @Test
    void desactivarCambiaElEstadoDeLaTienda() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");
        when(tiendaRepositoryPort.buscarPorId(tienda.getId())).thenReturn(Optional.of(tienda));
        when(tiendaRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.desactivar(tienda.getId());

        ArgumentCaptor<Tienda> captor = ArgumentCaptor.forClass(Tienda.class);
        verify(tiendaRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().estaActiva()).isFalse();
        verify(eventos).publishEvent(new TiendaDesactivada(tienda.getId(), captor.getValue().getActualizadoEn()));
    }

    @Test
    void desactivarUnaTiendaYaInactivaNoPublicaDeNuevo() {
        Tienda tienda = Tienda.reconstruir(UUID.randomUUID(), "Tienda Centro", "Bogotá",
                EstadoTienda.INACTIVA, Instant.now(), Instant.now());
        when(tiendaRepositoryPort.buscarPorId(tienda.getId())).thenReturn(Optional.of(tienda));

        servicio.desactivar(tienda.getId());

        verify(tiendaRepositoryPort, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void activarCambiaElEstadoDeLaTienda() {
        Tienda tienda = Tienda.reconstruir(UUID.randomUUID(), "Tienda Centro", "Bogotá",
                EstadoTienda.INACTIVA, Instant.now(), Instant.now());
        when(tiendaRepositoryPort.buscarPorId(tienda.getId())).thenReturn(Optional.of(tienda));
        when(tiendaRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Tienda activada = servicio.activar(tienda.getId());

        assertThat(activada.estaActiva()).isTrue();
    }

    @Test
    void laConsultaPublicaResumeLaTiendaSinExponerElDominio() {
        UUID id = UUID.randomUUID();
        Tienda inactiva = Tienda.reconstruir(id, "Centro", "Medellín", EstadoTienda.INACTIVA, Instant.now(), Instant.now());
        when(tiendaRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(inactiva));

        assertThat(servicio.buscar(id)).contains(new TiendaResumen(id, "Centro", false));
    }

    @Test
    void laConsultaPublicaDevuelveVacioSiLaTiendaNoExiste() {
        UUID id = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThat(servicio.buscar(id)).isEmpty();
    }

    @Test
    void laConsultaPublicaResumeVariasTiendasEnUnaSolaBusqueda() {
        UUID id = UUID.randomUUID();
        Tienda activa = Tienda.reconstruir(id, "Norte", null, EstadoTienda.ACTIVA, Instant.now(), Instant.now());
        when(tiendaRepositoryPort.buscarPorIds(List.of(id))).thenReturn(List.of(activa));

        assertThat(servicio.buscarTodas(List.of(id))).containsExactly(new TiendaResumen(id, "Norte", true));
    }

    @Test
    void consultaNodosDevuelveElIdDelAlmacenDeLaTienda() {
        UUID tiendaId = UUID.randomUUID();
        Nodo almacen = Nodo.crearParaTienda(tiendaId, TipoNodo.ALMACEN);
        when(nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN))
                .thenReturn(Optional.of(almacen));

        assertThat(servicio.buscarAlmacen(tiendaId)).contains(almacen.getId());
        verify(nodoRepositoryPort).buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN);
    }

    @Test
    void consultaNodosNoUsaLaBodegaTiendaComoAlmacen() {
        UUID tiendaId = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN))
                .thenReturn(Optional.empty());

        assertThat(servicio.buscarAlmacen(tiendaId)).isEmpty();
        verify(nodoRepositoryPort).buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN);
    }
}
