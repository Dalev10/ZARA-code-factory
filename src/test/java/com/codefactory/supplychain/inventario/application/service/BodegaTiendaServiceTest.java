package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaConInventarioAsociadoException;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaInactivaException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.model.EstadoTienda;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.when;

class BodegaTiendaServiceTest {

    private NodoRepositoryPort nodoRepositoryPort;
    private TiendaRepositoryPort tiendaRepositoryPort;
    private InventarioPorNodoPort inventarioPorNodoPort;
    private BodegaTiendaService servicio;

    @BeforeEach
    void setUp() {
        nodoRepositoryPort = mock(NodoRepositoryPort.class);
        tiendaRepositoryPort = mock(TiendaRepositoryPort.class);
        inventarioPorNodoPort = mock(InventarioPorNodoPort.class);
        servicio = new BodegaTiendaService(nodoRepositoryPort, tiendaRepositoryPort, inventarioPorNodoPort);
    }

    private static Tienda tiendaActiva(UUID id) {
        Instant ahora = Instant.now();
        return Tienda.reconstruir(id, "Tienda Centro", "Bogotá", EstadoTienda.ACTIVA, ahora, ahora);
    }

    private static Tienda tiendaInactiva(UUID id) {
        Instant ahora = Instant.now();
        return Tienda.reconstruir(id, "Tienda Centro", "Bogotá", EstadoTienda.INACTIVA, ahora, ahora);
    }

    @Test
    void registrarCreaElNodoBodegaTiendaParaUnaTiendaActivaSinBodegaPrevia() {
        UUID tiendaId = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.of(tiendaActiva(tiendaId)));
        when(nodoRepositoryPort.existePorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)).thenReturn(false);
        when(nodoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Nodo nodo = servicio.registrar(tiendaId);

        assertThat(nodo.getTipo()).isEqualTo(TipoNodo.BODEGA_TIENDA);
        assertThat(nodo.getTiendaId()).isEqualTo(tiendaId);
    }

    @Test
    void rechazaRegistrarSiLaTiendaNoExiste() {
        UUID tiendaId = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(tiendaId)).isInstanceOf(TiendaNoEncontradaException.class);

        verify(nodoRepositoryPort, never()).guardar(any());
    }

    @Test
    void rechazaRegistrarSiLaTiendaEstaInactiva() {
        UUID tiendaId = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.of(tiendaInactiva(tiendaId)));

        assertThatThrownBy(() -> servicio.registrar(tiendaId)).isInstanceOf(TiendaInactivaException.class);

        verify(nodoRepositoryPort, never()).guardar(any());
    }

    @Test
    void rechazaRegistrarSiLaTiendaYaTieneUnaBodega() {
        UUID tiendaId = UUID.randomUUID();
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.of(tiendaActiva(tiendaId)));
        when(nodoRepositoryPort.existePorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(tiendaId)).isInstanceOf(BodegaTiendaYaExisteException.class);
    }

    @Test
    void consultarPorIdRechazaUnNodoInexistente() {
        UUID id = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarPorId(id)).isInstanceOf(BodegaTiendaNoEncontradaException.class);
    }

    @Test
    void consultarPorIdRechazaUnNodoQueNoEsBodegaTienda() {
        UUID id = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(Nodo.crearParaCd(UUID.randomUUID())));

        assertThatThrownBy(() -> servicio.consultarPorId(id)).isInstanceOf(BodegaTiendaNoEncontradaException.class);
    }

    @Test
    void consultarPorIdEnriqueceConTiendaEInventario() {
        UUID tiendaId = UUID.randomUUID();
        Tienda tienda = tiendaActiva(tiendaId);
        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA);
        InventarioResumen resumen = new InventarioResumen(UUID.randomUUID(), 10, 8);

        when(nodoRepositoryPort.buscarPorId(nodo.getId())).thenReturn(Optional.of(nodo));
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.of(tienda));
        when(inventarioPorNodoPort.consultarPorNodo(nodo.getId())).thenReturn(List.of(resumen));

        BodegaTiendaConsulta consulta = servicio.consultarPorId(nodo.getId());

        assertThat(consulta.nodo()).isEqualTo(nodo);
        assertThat(consulta.tienda()).isEqualTo(tienda);
        assertThat(consulta.inventario()).containsExactly(resumen);
    }

    @Test
    void listarTodasDelegaAlPuertoFiltrandoPorTipo() {
        Nodo nodo = Nodo.crearParaTienda(UUID.randomUUID(), TipoNodo.BODEGA_TIENDA);
        when(nodoRepositoryPort.listarPorTipo(TipoNodo.BODEGA_TIENDA)).thenReturn(List.of(nodo));

        assertThat(servicio.listarTodas()).containsExactly(nodo);
    }

    @Test
    void modificarPermiteConservarLaMismaTiendaSinRevalidarNada() {
        UUID tiendaId = UUID.randomUUID();
        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA);
        when(nodoRepositoryPort.buscarPorId(nodo.getId())).thenReturn(Optional.of(nodo));
        when(nodoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.modificar(nodo.getId(), tiendaId);

        verify(tiendaRepositoryPort, never()).buscarPorId(any());
    }

    @Test
    void modificarValidaLaNuevaTiendaCuandoCambia() {
        UUID tiendaOriginalId = UUID.randomUUID();
        UUID tiendaNuevaId = UUID.randomUUID();
        Nodo nodo = Nodo.crearParaTienda(tiendaOriginalId, TipoNodo.BODEGA_TIENDA);
        when(nodoRepositoryPort.buscarPorId(nodo.getId())).thenReturn(Optional.of(nodo));
        when(tiendaRepositoryPort.buscarPorId(tiendaNuevaId)).thenReturn(Optional.of(tiendaActiva(tiendaNuevaId)));
        when(nodoRepositoryPort.existePorTiendaYTipo(tiendaNuevaId, TipoNodo.BODEGA_TIENDA)).thenReturn(false);
        when(nodoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Nodo actualizado = servicio.modificar(nodo.getId(), tiendaNuevaId);

        assertThat(actualizado.getTiendaId()).isEqualTo(tiendaNuevaId);
    }

    @Test
    void eliminarRechazaSiHayInventarioAsociado() {
        UUID id = UUID.randomUUID();
        Nodo nodo = Nodo.crearParaTienda(UUID.randomUUID(), TipoNodo.BODEGA_TIENDA);
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(
                Nodo.reconstruir(id, TipoNodo.BODEGA_TIENDA, null, nodo.getTiendaId())));
        when(inventarioPorNodoPort.consultarPorNodo(id))
                .thenReturn(List.of(new InventarioResumen(UUID.randomUUID(), 5, 5)));

        assertThatThrownBy(() -> servicio.eliminar(id))
                .isInstanceOf(BodegaTiendaConInventarioAsociadoException.class);

        verify(nodoRepositoryPort, never()).eliminar(any());
    }

    @Test
    void eliminarFuncionaSinInventarioAsociado() {
        UUID id = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(
                Nodo.reconstruir(id, TipoNodo.BODEGA_TIENDA, null, UUID.randomUUID())));
        when(inventarioPorNodoPort.consultarPorNodo(id)).thenReturn(List.of());

        servicio.eliminar(id);

        verify(nodoRepositoryPort).eliminar(id);
    }
}
