package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class TiendaServiceTest {

    private TiendaRepositoryPort tiendaRepositoryPort;
    private NodoRepositoryPort nodoRepositoryPort;
    private TiendaService servicio;

    @BeforeEach
    void setUp() {
        tiendaRepositoryPort = mock(TiendaRepositoryPort.class);
        nodoRepositoryPort = mock(NodoRepositoryPort.class);
        servicio = new TiendaService(tiendaRepositoryPort, nodoRepositoryPort);
    }

    @Test
    void crearGuardaLaTiendaYAprovisionaSuNodoAlmacen() {
        when(tiendaRepositoryPort.existePorNombre("Tienda Centro")).thenReturn(false);
        when(tiendaRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Tienda tienda = servicio.crear("Tienda Centro", "Bogotá");

        assertThat(tienda.getNombre()).isEqualTo("Tienda Centro");

        ArgumentCaptor<Nodo> captor = ArgumentCaptor.forClass(Nodo.class);
        verify(nodoRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNodo.ALMACEN);
        assertThat(captor.getValue().getTiendaId()).isEqualTo(tienda.getId());
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
        when(tiendaRepositoryPort.listarTodas()).thenReturn(List.of(tienda));

        assertThat(servicio.listar()).containsExactly(tienda);
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
    }
}
