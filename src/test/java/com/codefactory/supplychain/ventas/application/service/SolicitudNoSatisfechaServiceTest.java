package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase;
import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.SolicitudNoSatisfechaRepositoryPort;
import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import com.codefactory.supplychain.ventas.application.port.out.VarianteCatalogoPort;
import com.codefactory.supplychain.ventas.domain.exception.AccesoTiendaProhibidoException;
import com.codefactory.supplychain.ventas.domain.exception.ClaveIdempotenciaReutilizadaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.exception.TiendaSinAlmacenException;
import com.codefactory.supplychain.ventas.domain.exception.VarianteNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitudNoSatisfechaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T10:00:00Z");
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    private SolicitudNoSatisfechaRepositoryPort repositorio;
    private VarianteCatalogoPort catalogo;
    private AlmacenTiendaPort almacenes;
    private StockAlmacenPort stock;
    private SolicitudNoSatisfechaService servicio;
    private UsuarioActual usuario;

    @BeforeEach
    void setUp() {
        repositorio = mock(SolicitudNoSatisfechaRepositoryPort.class);
        catalogo = mock(VarianteCatalogoPort.class);
        almacenes = mock(AlmacenTiendaPort.class);
        stock = mock(StockAlmacenPort.class);
        servicio = new SolicitudNoSatisfechaService(repositorio, catalogo, almacenes, stock,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        usuario = new UsuarioActual(usuarioId, Set.of(), Set.of(tiendaId));
    }

    @Test
    void registraConCantidadPorDefectoYConservaStockObservadoAunqueSeaPositivo() {
        prepararRegistro();
        when(stock.aLaMano(almacenId, varianteId)).thenReturn(Optional.of(8));
        when(repositorio.insertarSiAusente(any())).thenReturn(true);

        var resultado = servicio.registrar(comando(null, "clave-1"));

        assertThat(resultado.creada()).isTrue();
        assertThat(resultado.solicitud().getCantidad()).isEqualTo(1);
        assertThat(resultado.solicitud().getStockSistema()).isEqualTo(8);
        assertThat(resultado.solicitud().getRegistradaEn()).isEqualTo(AHORA);
        verify(repositorio).insertarSiAusente(any());
    }

    @Test
    void registraCantidadIndicadaYStockDesconocido() {
        prepararRegistro();
        when(stock.aLaMano(almacenId, varianteId)).thenReturn(Optional.empty());
        when(repositorio.insertarSiAusente(any())).thenReturn(true);

        var resultado = servicio.registrar(comando(4, "clave-2"));

        assertThat(resultado.solicitud().getCantidad()).isEqualTo(4);
        assertThat(resultado.solicitud().getStockSistema()).isNull();
    }

    @Test
    void devuelveRepeticionIdempotenteAntesDeConsultarCatalogo() {
        SolicitudNoSatisfecha previa = solicitud(2, "clave-3");
        when(repositorio.buscarPorClave("clave-3")).thenReturn(Optional.of(previa));

        var resultado = servicio.registrar(comando(2, "clave-3"));

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.solicitud()).isSameAs(previa);
        verify(catalogo, never()).existe(any());
    }

    @Test
    void rechazaReutilizarClaveConDatosDiferentes() {
        when(repositorio.buscarPorClave("clave-4")).thenReturn(Optional.of(solicitud(2, "clave-4")));

        assertThatThrownBy(() -> servicio.registrar(comando(3, "clave-4")))
                .isInstanceOf(ClaveIdempotenciaReutilizadaException.class);
    }

    @Test
    void resuelveCarreraDeInsercionComoRepeticion() {
        prepararRegistro();
        when(stock.aLaMano(almacenId, varianteId)).thenReturn(Optional.of(0));
        when(repositorio.insertarSiAusente(any())).thenReturn(false);
        SolicitudNoSatisfecha ganadora = solicitud(2, "clave-5");
        when(repositorio.buscarPorClave("clave-5"))
                .thenReturn(Optional.empty(), Optional.of(ganadora));

        var resultado = servicio.registrar(comando(2, "clave-5"));

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.solicitud()).isSameAs(ganadora);
    }

    @Test
    void validaCantidadClaveAccesoVarianteYAlmacen() {
        assertThatThrownBy(() -> servicio.registrar(comando(0, "k")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.registrar(comando(1, " ")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.registrar(new SolicitudNoSatisfechaUseCase.RegistrarComando(
                usuario, UUID.randomUUID(), varianteId, 1, "k")))
                .isInstanceOf(AccesoTiendaProhibidoException.class);

        when(repositorio.buscarPorClave("k")).thenReturn(Optional.empty());
        when(catalogo.existe(varianteId)).thenReturn(false);
        assertThatThrownBy(() -> servicio.registrar(comando(1, "k")))
                .isInstanceOf(VarianteNoEncontradaException.class);

        when(catalogo.existe(varianteId)).thenReturn(true);
        when(almacenes.almacenDeTienda(tiendaId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.registrar(comando(1, "k")))
                .isInstanceOf(TiendaSinAlmacenException.class);
    }

    @Test
    void consultaDentroDelAlcanceYDevuelveVacioParaTiendaAjena() {
        Pageable pageable = PageRequest.of(0, 5);
        var filtros = new SolicitudNoSatisfechaUseCase.ConsultaFiltros(
                null, varianteId, EstadoSolicitud.ACTIVA, AHORA.minusSeconds(60), AHORA);
        when(repositorio.buscar(any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(solicitud(1, "q"))));

        assertThat(servicio.consultar(usuario, filtros, pageable)).hasSize(1);
        assertThat(servicio.consultar(usuario,
                new SolicitudNoSatisfechaUseCase.ConsultaFiltros(UUID.randomUUID(), null, null, null, null),
                pageable)).isEmpty();
        verify(repositorio).buscar(any(), eq(pageable));
    }

    @Test
    void obtenerYAnularRestringenPorTiendaYRegistranAuditoria() {
        SolicitudNoSatisfecha solicitud = solicitud(2, "clave-6");
        when(repositorio.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(repositorio.guardar(solicitud)).thenReturn(solicitud);

        assertThat(servicio.obtener(usuario, solicitud.getId())).isSameAs(solicitud);
        assertThat(servicio.anular(usuario, solicitud.getId(), "Error de registro")).isSameAs(solicitud);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(solicitud.getAnuladaPor()).isEqualTo(usuarioId);
        assertThat(solicitud.getAnuladaEn()).isEqualTo(AHORA);
        assertThat(solicitud.getMotivoAnulacion()).isEqualTo("Error de registro");

        // Igual que la venta (D-44): repetir la anulación devuelve la original sin guardar otra vez.
        assertThat(servicio.anular(usuario, solicitud.getId(), "Otro motivo").getMotivoAnulacion())
                .isEqualTo("Error de registro");
        verify(repositorio, times(1)).guardar(solicitud);
        assertThatThrownBy(() -> servicio.anular(usuario, solicitud.getId(), " "))
                .isInstanceOf(SolicitudInvalidaException.class);

        assertThatThrownBy(() -> servicio.obtener(
                new UsuarioActual(UUID.randomUUID(), Set.of(), Set.of()), solicitud.getId()))
                .isInstanceOf(SolicitudNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.obtener(usuario, UUID.randomUUID()))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    private void prepararRegistro() {
        when(repositorio.buscarPorClave(any())).thenReturn(Optional.empty());
        when(catalogo.existe(varianteId)).thenReturn(true);
        when(almacenes.almacenDeTienda(tiendaId)).thenReturn(Optional.of(almacenId));
    }

    private SolicitudNoSatisfechaUseCase.RegistrarComando comando(Integer cantidad, String clave) {
        return new SolicitudNoSatisfechaUseCase.RegistrarComando(usuario, tiendaId, varianteId, cantidad, clave);
    }

    private SolicitudNoSatisfecha solicitud(int cantidad, String clave) {
        return SolicitudNoSatisfecha.registrar(tiendaId, varianteId, cantidad, 0, AHORA, usuarioId, clave);
    }
}
