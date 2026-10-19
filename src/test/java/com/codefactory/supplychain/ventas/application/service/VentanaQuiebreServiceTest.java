package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort;
import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort.CambioStock;
import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.AccesoTiendaProhibidoException;
import com.codefactory.supplychain.ventas.domain.exception.TiendaSinAlmacenException;
import com.codefactory.supplychain.ventas.domain.exception.VentanaInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.VentanaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentanaQuiebreServiceTest {

    private static final Instant INICIO = Instant.parse("2026-10-18T09:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-18T12:00:00Z");
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    private VentanaQuiebreRepositoryPort ventanas;
    private AlmacenTiendaPort almacenes;
    private StockAlmacenPort stock;
    private HistorialStockPort historial;
    private VentanaQuiebreService servicio;
    private UsuarioActual usuario;

    @BeforeEach
    void setUp() {
        ventanas = mock(VentanaQuiebreRepositoryPort.class);
        almacenes = mock(AlmacenTiendaPort.class);
        stock = mock(StockAlmacenPort.class);
        historial = mock(HistorialStockPort.class);
        servicio = new VentanaQuiebreService(ventanas, almacenes, stock, historial,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        usuario = new UsuarioActual(usuarioId, Set.of(), Set.of(tiendaId));
        when(almacenes.almacenDeTienda(tiendaId)).thenReturn(Optional.of(almacenId));
    }

    @Test
    void reconstruyePeriodosCerradosYAbiertosDesdeCambiosOrdenados() {
        Instant cierre = INICIO.plusSeconds(60);
        Instant segundoInicio = INICIO.plusSeconds(120);
        List<CambioStock> cambios = List.of(
                cambio(INICIO, -5, -2),
                cambio(cierre, 3, 1),
                cambio(segundoInicio, -2, -1));

        assertThat(VentanaQuiebreService.reconstruir(cambios))
                .containsExactly(
                        new VentanaQuiebreService.Periodo(INICIO, cierre),
                        new VentanaQuiebreService.Periodo(segundoInicio, null));
        assertThat(VentanaQuiebreService.reconstruir(List.of())).isEmpty();
    }

    @Test
    void creaVentanaCerradaUsandoElKardexAunqueLlegueDesordenado() {
        Instant fin = INICIO.plusSeconds(60);
        when(historial.cambios(eq(almacenId), eq(varianteId), any(), any()))
                .thenReturn(List.of(cambio(fin, 3, 1), cambio(INICIO, -5, -2)));

        var resultado = servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA));

        assertThat(resultado.paresProcesados()).isEqualTo(1);
        assertThat(resultado.creadas()).isEqualTo(1);
        verify(ventanas).guardar(any(VentanaQuiebre.class));
    }

    @Test
    void creaVentanaAbiertaYCuentaLaColisionConOtraVentana() {
        when(historial.cambios(eq(almacenId), eq(varianteId), any(), any()))
                .thenReturn(List.of(cambio(INICIO, -2, -1)));
        when(ventanas.abrirSiAusente(any())).thenReturn(true);
        assertThat(servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA)).creadas()).isEqualTo(1);

        when(ventanas.abrirSiAusente(any())).thenReturn(false);
        var resultado = servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA));
        assertThat(resultado.creadas()).isZero();
    }

    @Test
    void cierraAbiertaCorrigeCierreReposicionYRespetaOtrosMotivos() {
        Instant fin = INICIO.plusSeconds(90);
        VentanaQuiebre abierta = VentanaQuiebre.abrir(tiendaId, varianteId, INICIO);
        when(historial.cambios(eq(almacenId), eq(varianteId), any(), any()))
                .thenReturn(List.of(cambio(INICIO, -4, -1), cambio(fin, 3, 2)));
        when(ventanas.listarPorTiendaYVariante(tiendaId, varianteId)).thenReturn(List.of(abierta));

        assertThat(servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA)).cerradas()).isEqualTo(1);
        assertThat(abierta.getFechaFin()).isEqualTo(fin);

        VentanaQuiebre reposicion = VentanaQuiebre.reconstituir(UUID.randomUUID(), tiendaId, varianteId,
                INICIO, INICIO.plusSeconds(20), MotivoCierreVentana.REPOSICION);
        when(ventanas.listarPorTiendaYVariante(tiendaId, varianteId)).thenReturn(List.of(reposicion));
        var corregida = servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA));
        assertThat(corregida.corregidas()).isEqualTo(1);
        assertThat(reposicion.getFechaFin()).isEqualTo(fin);

        VentanaQuiebre forzada = VentanaQuiebre.reconstituir(UUID.randomUUID(), tiendaId, varianteId,
                INICIO, INICIO.plusSeconds(20), MotivoCierreVentana.VENTA_FORZADA);
        when(ventanas.listarPorTiendaYVariante(tiendaId, varianteId)).thenReturn(List.of(forzada));
        var sinCambio = servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA));
        assertThat(sinCambio.sinCambios()).isEqualTo(1);
        assertThat(forzada.getFechaFin()).isEqualTo(INICIO.plusSeconds(20));
    }

    @Test
    void reportaVentanasSinRespaldoYAbiertasQueNoCoincidenConElKardex() {
        VentanaQuiebre noRespaldada = VentanaQuiebre.abrir(tiendaId, varianteId, INICIO.plusSeconds(30));
        when(historial.cambios(eq(almacenId), eq(varianteId), any(), any())).thenReturn(List.of());
        when(ventanas.listarPorTiendaYVariante(tiendaId, varianteId)).thenReturn(List.of(noRespaldada));

        var resultado = servicio.reconciliar(comando(varianteId, Instant.EPOCH, AHORA));

        assertThat(resultado.sinRespaldoEnKardex()).isEqualTo(1);
        verify(ventanas, never()).guardar(any());
    }

    @Test
    void procesaUnionDeVariantesDeStockYVentanasSinDuplicarlas() {
        UUID otraVariante = UUID.randomUUID();
        when(stock.variantesConExistencias(almacenId)).thenReturn(List.of(varianteId));
        when(ventanas.variantesConVentanas(tiendaId)).thenReturn(List.of(varianteId, otraVariante));
        when(historial.cambios(any(), any(), any(), any())).thenReturn(List.of());
        when(ventanas.listarPorTiendaYVariante(any(), any())).thenReturn(List.of());

        var resultado = servicio.reconciliar(comando(null, null, null));

        assertThat(resultado.paresProcesados()).isEqualTo(2);
        verify(historial).cambios(almacenId, varianteId, Instant.EPOCH, AHORA);
        verify(historial).cambios(almacenId, otraVariante, Instant.EPOCH, AHORA);
    }

    @Test
    void validaAlcanceRangoYTiendaConAlmacen() {
        assertThatThrownBy(() -> servicio.reconciliar(
                new VentanaQuiebreUseCase.ReconciliarComando(usuario, null, varianteId, null, null)))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> servicio.reconciliar(new VentanaQuiebreUseCase.ReconciliarComando(
                new UsuarioActual(UUID.randomUUID(), Set.of(), Set.of()), tiendaId, varianteId, null, null)))
                .isInstanceOf(AccesoTiendaProhibidoException.class);
        assertThatThrownBy(() -> servicio.reconciliar(comando(
                varianteId, AHORA, INICIO)))
                .isInstanceOf(VentanaInvalidaException.class);
        when(almacenes.almacenDeTienda(tiendaId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.reconciliar(comando(varianteId, null, null)))
                .isInstanceOf(TiendaSinAlmacenException.class);
    }

    @Test
    void consultaYObtieneSoloVentanasDentroDelAlcance() {
        Pageable pageable = PageRequest.of(0, 10);
        VentanaQuiebre ventana = VentanaQuiebre.abrir(tiendaId, varianteId, INICIO);
        when(ventanas.buscar(any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(ventana)));
        when(ventanas.buscarPorId(ventana.getId())).thenReturn(Optional.of(ventana));

        assertThat(servicio.consultar(usuario,
                new VentanaQuiebreUseCase.ConsultaFiltros(null, varianteId, true, INICIO, AHORA), pageable))
                .hasSize(1);
        assertThat(servicio.consultar(usuario,
                new VentanaQuiebreUseCase.ConsultaFiltros(UUID.randomUUID(), null, null, null, null), pageable))
                .isEmpty();
        assertThat(servicio.obtener(usuario, ventana.getId())).isSameAs(ventana);
        assertThatThrownBy(() -> servicio.obtener(
                new UsuarioActual(UUID.randomUUID(), Set.of(), Set.of()), ventana.getId()))
                .isInstanceOf(VentanaNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.obtener(usuario, UUID.randomUUID()))
                .isInstanceOf(VentanaNoEncontradaException.class);
    }

    private VentanaQuiebreUseCase.ReconciliarComando comando(UUID variante, Instant desde, Instant hasta) {
        return new VentanaQuiebreUseCase.ReconciliarComando(usuario, tiendaId, variante, desde, hasta);
    }

    private CambioStock cambio(Instant fecha, int cantidad, int saldo) {
        return new CambioStock(UUID.randomUUID(), "AJUSTE", cantidad, saldo, fecha);
    }
}
