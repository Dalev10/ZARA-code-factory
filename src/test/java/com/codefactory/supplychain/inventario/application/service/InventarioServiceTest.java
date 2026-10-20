package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class InventarioServiceTest {

    private InventarioRepositoryPort repository;
    private MovimientoInventarioRepositoryPort movimientoRepository;
    private NodoRepositoryPort nodoRepository;
    private org.springframework.context.ApplicationEventPublisher eventos;
    private InventarioService servicio;

    private final UUID tiendaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(InventarioRepositoryPort.class);
        movimientoRepository = mock(MovimientoInventarioRepositoryPort.class);
        nodoRepository = mock(NodoRepositoryPort.class);
        eventos = mock(org.springframework.context.ApplicationEventPublisher.class);
        when(nodoRepository.buscarPorId(any())).thenAnswer(invocacion -> Optional.of(
                Nodo.reconstruir(invocacion.getArgument(0), TipoNodo.ALMACEN, null, tiendaId)));
        servicio = new InventarioService(repository, movimientoRepository, nodoRepository, eventos);
    }

    /**
     * Escenario: sin fila, el stock es 0 (D-25)
     *   Dado un nodo sin fila para la variante X
     *   Cuando se descuenta X sin forzar
     *   Entonces se rechaza por stock insuficiente
     *   Y no se crea ninguna fila
     */
    @Test
    void ventaSinFilaYSinForzarSeRechazaPorStockInsuficiente() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicarVenta(nodoId, varianteId, 1, false, UUID.randomUUID()))
                .isInstanceOf(StockInsuficienteException.class);
        verify(repository, never()).guardar(any());
        verifyNoInteractions(movimientoRepository, eventos);
    }

    /**
     * Escenario: venta forzada de una variante sin fila (D-25, D-35)
     *   Dado un nodo sin fila para la variante X
     *   Cuando se descuenta 1 unidad de X con forzado = true
     *   Entonces se crea la fila con a la mano -1
     *   Y el kardex registra la venta con su referencia
     */
    @Test
    void ventaForzadaSinFilaCreaLaFilaEnNegativo() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        MovimientoInventario movimiento = servicio.aplicarVenta(nodoId, varianteId, 1, true, ventaId);

        assertThat(movimiento.saldoResultante()).isEqualTo(-1);
        assertThat(movimiento.referenciaId()).isEqualTo(ventaId);
        verify(repository).guardar(org.mockito.ArgumentMatchers.argThat(inventario ->
                inventario.getVarianteId().equals(varianteId) && inventario.getALaMano() == -1));
        var evento = forClass(ExistenciasActualizadas.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().referenciaId()).isEqualTo(ventaId);
        assertThat(evento.getValue().aLaManoAnterior()).isZero();
    }

    /**
     * Escenario: traspaso Bodega → Almacén
     *   Dada una Bodega con 5 unidades y un Almacén sin fila
     *   Cuando se trasladan 3 unidades
     *   Entonces la Bodega queda en 2 y el Almacén en 3
     *   Y cada nodo registra su movimiento con el traspaso como referencia
     */
    @Test
    void trasladaEntreNodosYCreaLaFilaDestino() {
        UUID bodegaId = UUID.randomUUID();
        UUID almacenId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        UUID traspasoId = UUID.randomUUID();
        Inventario bodega = Inventario.crear(bodegaId, varianteId);
        bodega.cargar(5, null);
        when(repository.buscarPorNodoYVariante(bodegaId, varianteId)).thenReturn(Optional.of(bodega));
        when(repository.buscarPorNodoYVariante(almacenId, varianteId)).thenReturn(Optional.empty());

        servicio.trasladar(bodegaId, almacenId, varianteId, 3, traspasoId);

        assertThat(bodega.getALaMano()).isEqualTo(2);
        var movimientos = forClass(MovimientoInventario.class);
        verify(movimientoRepository).guardar(eq(bodegaId), eq(varianteId), movimientos.capture());
        verify(movimientoRepository).guardar(eq(almacenId), eq(varianteId), movimientos.capture());
        assertThat(movimientos.getAllValues())
                .extracting(MovimientoInventario::tipo, MovimientoInventario::cantidad,
                        MovimientoInventario::saldoResultante, MovimientoInventario::referenciaId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TipoMovimientoInventario.TRASPASO_SALIDA, -3, 2, traspasoId),
                        org.assertj.core.groups.Tuple.tuple(TipoMovimientoInventario.TRASPASO_ENTRADA, 3, 3, traspasoId));
        verify(eventos, org.mockito.Mockito.times(2)).publishEvent(any(ExistenciasActualizadas.class));
    }

    @Test
    void traspasoNoDejaElOrigenEnNegativo() {
        UUID bodegaId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(bodegaId, varianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.trasladar(bodegaId, UUID.randomUUID(), varianteId, 1, UUID.randomUUID()))
                .isInstanceOf(StockInsuficienteException.class);
        verify(repository, never()).guardar(any());
        verifyNoInteractions(movimientoRepository, eventos);
    }

    @Test
    void buscaLaFilaAntesDeAplicarLaVentaYGuardaElAgregado() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(5, null);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarVenta(nodoId, varianteId, 2, false, UUID.randomUUID());

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(inventario.getALaMano()).isEqualTo(3);
        verify(repository).guardar(same(inventario));
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        var evento = forClass(ExistenciasActualizadas.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().nodoId()).isEqualTo(nodoId);
        assertThat(evento.getValue().tipoNodo()).isEqualTo(TipoNodoInventario.ALMACEN);
        assertThat(evento.getValue().tiendaId()).isEqualTo(tiendaId);
        assertThat(evento.getValue().varianteId()).isEqualTo(varianteId);
        assertThat(evento.getValue().movimientoId()).isEqualTo(movimiento.id());
        assertThat(evento.getValue().tipoMovimiento().name()).isEqualTo(movimiento.tipo().name());
        assertThat(evento.getValue().aLaManoAnterior()).isEqualTo(5);
        assertThat(evento.getValue().aLaMano()).isEqualTo(3);
        assertThat(evento.getValue().disponibleParaUso()).isEqualTo(3);
        assertThat(evento.getValue().pronosticado()).isEqualTo(3);
        assertThat(evento.getValue().ocurridoEn()).isEqualTo(movimiento.ocurridoEn());
        assertThat(evento.getValue().forzado()).isFalse();
    }

    @Test
    void ventaForzadaConservaLaTrazabilidadEnElEvento() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(1, null);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarVenta(nodoId, varianteId, 3, true, UUID.randomUUID());

        assertThat(movimiento.forzado()).isTrue();
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        var evento = forClass(ExistenciasActualizadas.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().forzado()).isTrue();
        assertThat(evento.getValue().aLaManoAnterior()).isEqualTo(1);
        assertThat(evento.getValue().aLaMano()).isEqualTo(-2);
    }

    @Test
    void elEventoDeUnCdNoLlevaTienda() {
        UUID cdNodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(nodoRepository.buscarPorId(cdNodoId)).thenReturn(Optional.of(
                Nodo.reconstruir(cdNodoId, TipoNodo.CD, UUID.randomUUID(), null)));
        when(repository.buscarPorNodoYVariante(cdNodoId, varianteId)).thenReturn(Optional.empty());

        servicio.cargar(cdNodoId, varianteId, 4, null);

        var evento = forClass(ExistenciasActualizadas.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().tipoNodo()).isEqualTo(TipoNodoInventario.CD);
        assertThat(evento.getValue().tiendaId()).isNull();
        assertThat(evento.getValue().aLaManoAnterior()).isZero();
        assertThat(evento.getValue().aLaMano()).isEqualTo(4);
    }

    @Test
    void rechazaCargarEnUnNodoInexistenteSinGuardarNada() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(nodoRepository.buscarPorId(nodoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cargar(nodoId, varianteId, 4, null))
                .isInstanceOf(NodoNoEncontradoException.class);
        verify(repository, never()).guardar(any());
        verifyNoInteractions(movimientoRepository, eventos);
    }

    @Test
    void cargaEnCeroDejaLaVarianteCargada() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        MovimientoInventario movimiento = servicio.cargar(nodoId, varianteId, 0, null);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(movimiento.cantidad()).isZero();
        verify(repository).guardar(org.mockito.ArgumentMatchers.argThat(inventario ->
                inventario.getALaMano() == 0));
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        verify(eventos).publishEvent(any(ExistenciasActualizadas.class));
    }

    @Test
    void publicaEventoAlAplicarUnAjuste() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarAjuste(
                nodoId, varianteId, 3, MotivoAjuste.CONTEO_FISICO, null, null);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE);
        verify(movimientoRepository).guardar(eq(nodoId), eq(varianteId), same(movimiento));
        verify(eventos).publishEvent(any(ExistenciasActualizadas.class));
    }

    @Test
    void cargarCreaLaFilaParaLaCargaInicial() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        MovimientoInventario movimiento = servicio.cargar(nodoId, varianteId, 8, null);

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

        assertThatThrownBy(() -> servicio.cargar(nodoId, varianteId, 8, null))
                .isInstanceOf(InventarioYaCargadoException.class);
    }
}
