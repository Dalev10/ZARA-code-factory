package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.VarianteNoCargadaException;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class InventarioServiceTest {

    private InventarioRepositoryPort repository;
    private MovimientoInventarioRepositoryPort movimientoRepository;
    private org.springframework.context.ApplicationEventPublisher eventos;
    private InventarioService servicio;

    @BeforeEach
    void setUp() {
        repository = mock(InventarioRepositoryPort.class);
        movimientoRepository = mock(MovimientoInventarioRepositoryPort.class);
        eventos = mock(org.springframework.context.ApplicationEventPublisher.class);
        servicio = new InventarioService(repository, movimientoRepository, eventos);
    }

    @Test
    void rechazaOperarUnaVarianteNoCargada() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicarVenta(nodoId, varianteId, 1, false))
                .isInstanceOf(VarianteNoCargadaException.class);
    }

    @Test
    void buscaLaFilaAntesDeAplicarLaVentaYGuardaElAgregado() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(5);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarVenta(nodoId, varianteId, 2, false);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(inventario.getALaMano()).isEqualTo(3);
        verify(repository).guardar(same(inventario));
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        var evento = forClass(ExistenciasActualizadas.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().nodoId()).isEqualTo(nodoId);
        assertThat(evento.getValue().varianteId()).isEqualTo(varianteId);
        assertThat(evento.getValue().movimientoId()).isEqualTo(movimiento.id());
        assertThat(evento.getValue().tipoMovimiento().name()).isEqualTo(movimiento.tipo().name());
        assertThat(evento.getValue().aLaMano()).isEqualTo(3);
        assertThat(evento.getValue().disponibleParaUso()).isEqualTo(3);
        assertThat(evento.getValue().pronosticado()).isEqualTo(3);
        assertThat(evento.getValue().ocurridoEn()).isEqualTo(movimiento.ocurridoEn());
    }

    @Test
    void aplicaReposicionSoloSobreUnaFilaExistente() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.reponer(nodoId, varianteId, 4);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.REPOSICION);
        verify(repository).guardar(same(inventario));
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
    }

    @Test
    void publicaEventoAlAplicarUnAjuste() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarAjuste(nodoId, varianteId, 3, "conteo");

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE);
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        verify(eventos).publishEvent(any(ExistenciasActualizadas.class));
    }

    @Test
    void cargarCreaLaFilaParaLaCargaInicial() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        MovimientoInventario movimiento = servicio.cargar(nodoId, varianteId, 8);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        verify(repository).guardar(org.mockito.ArgumentMatchers.argThat(inventario ->
                inventario.getNodoId().equals(nodoId)
                        && inventario.getVarianteId().equals(varianteId)
                        && inventario.getALaMano() == 8));
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        verify(eventos).publishEvent(any(ExistenciasActualizadas.class));
    }

    @Test
    void rechazaUnaSegundaCargaInicial() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> servicio.cargar(nodoId, varianteId, 8))
                .isInstanceOf(InventarioYaCargadoException.class);
    }
}
