package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.out.ReintegrarStockPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.VentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnularVentaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T12:00:00Z");
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID transaccionId = UUID.randomUUID();
    private final UUID vendedorId = UUID.randomUUID();
    private final UUID anuladorId = UUID.randomUUID();

    private VentaRepositoryPort ventas;
    private TiendaVentaPort tiendas;
    private ReintegrarStockPort stock;
    private AnularVentaService servicio;
    private UsuarioActual usuario;

    @BeforeEach
    void setUp() {
        ventas = mock(VentaRepositoryPort.class);
        tiendas = mock(TiendaVentaPort.class);
        stock = mock(ReintegrarStockPort.class);
        servicio = new AnularVentaService(ventas, tiendas, stock, Clock.fixed(AHORA, ZoneOffset.UTC));
        usuario = new UsuarioActual(anuladorId, Set.of("ventas:anular"), Set.of(tiendaId));
        when(tiendas.buscar(tiendaId)).thenReturn(Optional.of(
                new TiendaVentaPort.TiendaVenta(tiendaId, true, almacenId)));
    }

    @Test
    void anulaLineaRestauraStockYConservaAuditoriaDelVendedor() {
        Venta original = venta(UUID.randomUUID());
        when(ventas.buscarPorIdParaActualizar(original.id())).thenReturn(Optional.of(original));
        when(ventas.guardar(org.mockito.ArgumentMatchers.any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Venta anulada = servicio.anular(usuario, original.id(), "Error de captura");

        assertThat(anulada.vendedorId()).isEqualTo(vendedorId);
        assertThat(anulada.anuladaPor()).isEqualTo(anuladorId);
        assertThat(anulada.estado().name()).isEqualTo("ANULADA");
        verify(stock).reintegrar(almacenId, original.varianteId(), original.cantidad(), original.id());
    }

    @Test
    void anulaTransaccionCompletaConTodoElStock() {
        Venta primera = venta(UUID.randomUUID());
        Venta segunda = venta(UUID.randomUUID());
        when(ventas.buscarPorTransaccionParaActualizar(transaccionId)).thenReturn(List.of(primera, segunda));
        when(ventas.guardarTodas(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        List<Venta> anuladas = servicio.anularTransaccion(usuario, transaccionId, "Venta duplicada");

        assertThat(anuladas).hasSize(2).allSatisfy(v -> assertThat(v.anuladaPor()).isEqualTo(anuladorId));
        verify(stock).reintegrar(almacenId, primera.varianteId(), primera.cantidad(), primera.id());
        verify(stock).reintegrar(almacenId, segunda.varianteId(), segunda.cantidad(), segunda.id());
    }

    @Test
    void validaTodasLasLineasAntesDeTocarStock() {
        Venta activa = venta(UUID.randomUUID());
        Venta otraTienda = Venta.registrarPorApi(transaccionId, UUID.randomUUID(), UUID.randomUUID(), 3,
                new BigDecimal("10.00"), "FISICO", vendedorId, false, AHORA);
        when(ventas.buscarPorTransaccionParaActualizar(transaccionId)).thenReturn(List.of(activa, otraTienda));

        assertThatThrownBy(() -> servicio.anularTransaccion(usuario, transaccionId, "Anulación"))
                .isInstanceOf(VentaNoEncontradaException.class);
        verify(stock, never()).reintegrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(ventas, never()).guardarTodas(anyList());
    }

    @Test
    void ocultaAnulacionDeVentaDeOtraTienda() {
        Venta original = venta(UUID.randomUUID());
        when(ventas.buscarPorIdParaActualizar(original.id())).thenReturn(Optional.of(original));
        UsuarioActual usuarioAjeno = new UsuarioActual(anuladorId, Set.of("ventas:anular"), Set.of());

        assertThatThrownBy(() -> servicio.anular(usuarioAjeno, original.id(), "Motivo"))
                .isInstanceOf(VentaNoEncontradaException.class);
        verify(stock, never()).reintegrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repetirAnulacionConfirmadaNoVuelveAReintegrarStock() {
        Venta anulada = venta(UUID.randomUUID()).anular(anuladorId, AHORA, "Error de captura");
        when(ventas.buscarPorIdParaActualizar(anulada.id())).thenReturn(Optional.of(anulada));

        Venta repetida = servicio.anular(usuario, anulada.id(), "Error de captura");

        assertThat(repetida).isEqualTo(anulada);
        verify(stock, never()).reintegrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(ventas, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repetirAnulacionConfirmadaSigueValidandoElAccesoALaTienda() {
        Venta anulada = venta(UUID.randomUUID()).anular(anuladorId, AHORA, "Error de captura");
        when(ventas.buscarPorIdParaActualizar(anulada.id())).thenReturn(Optional.of(anulada));
        UsuarioActual usuarioAjeno = new UsuarioActual(anuladorId, Set.of("ventas:anular"), Set.of());

        assertThatThrownBy(() -> servicio.anular(usuarioAjeno, anulada.id(), "Error de captura"))
                .isInstanceOf(VentaNoEncontradaException.class);
        verify(stock, never()).reintegrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(ventas, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repetirAnulacionConfirmadaDeTransaccionNoVuelveAReintegrarStock() {
        Venta anulada = venta(UUID.randomUUID()).anular(anuladorId, AHORA, "Venta duplicada");
        when(ventas.buscarPorTransaccionParaActualizar(transaccionId)).thenReturn(List.of(anulada));

        List<Venta> repetidas = servicio.anularTransaccion(usuario, transaccionId, "Venta duplicada");

        assertThat(repetidas).containsExactly(anulada);
        verify(stock, never()).reintegrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(ventas, never()).guardarTodas(anyList());
    }

    @Test
    void nuevaAnulacionDeTransaccionNoReintegraDeNuevoLineasYaAnuladas() {
        Venta anulada = venta(UUID.randomUUID()).anular(anuladorId, AHORA, "Ya anulada");
        Venta activa = venta(UUID.randomUUID());
        when(ventas.buscarPorTransaccionParaActualizar(transaccionId)).thenReturn(List.of(anulada, activa));
        when(ventas.guardarTodas(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        List<Venta> resultado = servicio.anularTransaccion(usuario, transaccionId, "Completar anulación");

        assertThat(resultado).hasSize(2).allSatisfy(v -> assertThat(v.estado()).isEqualTo(EstadoVenta.ANULADA));
        verify(stock).reintegrar(almacenId, activa.varianteId(), activa.cantidad(), activa.id());
        verify(stock, never()).reintegrar(almacenId, anulada.varianteId(), anulada.cantidad(), anulada.id());
        verify(ventas).guardarTodas(org.mockito.ArgumentMatchers.<List<Venta>>argThat(lineas ->
                lineas.size() == 1 && lineas.get(0).id().equals(activa.id())));
    }

    private Venta venta(UUID varianteId) {
        return Venta.registrarPorApi(transaccionId, varianteId, tiendaId, 3, new BigDecimal("10.00"),
                "FISICO", vendedorId, false, AHORA);
    }
}
