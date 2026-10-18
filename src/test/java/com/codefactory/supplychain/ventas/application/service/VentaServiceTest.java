package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Comando;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Item;
import com.codefactory.supplychain.ventas.application.port.out.DescontarStockPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.VarianteVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.OperacionVentaProhibidaException;
import com.codefactory.supplychain.ventas.domain.exception.SinStockException;
import com.codefactory.supplychain.ventas.domain.exception.TransaccionVentaReutilizadaException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T12:00:00Z");
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID transaccionId = UUID.randomUUID();
    private final UUID vendedorId = UUID.randomUUID();

    private VentaRepositoryPort ventas;
    private TiendaVentaPort tiendas;
    private VarianteVentaPort variantes;
    private DescontarStockPort stock;
    private VentaService servicio;

    @BeforeEach
    void setUp() {
        ventas = mock(VentaRepositoryPort.class);
        tiendas = mock(TiendaVentaPort.class);
        variantes = mock(VarianteVentaPort.class);
        stock = mock(DescontarStockPort.class);
        servicio = new VentaService(ventas, tiendas, variantes, stock,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void registraLoteDescuentaAlmacenYUsaAlVendedorDelToken() {
        prepararTiendaYVariante();
        when(ventas.guardarTodas(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.registrar(comando(false, Set.of()));

        assertThat(resultado.creada()).isTrue();
        verify(ventas).bloquearTransaccion(transaccionId);
        assertThat(resultado.lineas()).singleElement().satisfies(venta -> {
            assertThat(venta.vendedorId()).isEqualTo(vendedorId);
            assertThat(venta.canal()).isEqualTo("FISICO");
            assertThat(venta.timestampVenta()).isEqualTo(AHORA);
        });
        verify(stock).descontar(almacenId, varianteId, 2, resultado.lineas().get(0).id(), false);
    }

    @Test
    void reintentoIdenticoDevuelveVentaOriginalSinVolverADescontar() {
        Venta original = venta(false);
        when(variantes.buscarIdPorSku("SKU-1")).thenReturn(Optional.of(varianteId));
        when(ventas.buscarPorTransaccion(transaccionId)).thenReturn(List.of(original));

        var resultado = servicio.registrar(comando(false, Set.of()));

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.lineas()).containsExactly(original);
        verify(stock, never()).descontar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void rechazaReusarTransaccionConOtroPayload() {
        Venta original = venta(false);
        when(variantes.buscarIdPorSku("SKU-1")).thenReturn(Optional.of(varianteId));
        when(ventas.buscarPorTransaccion(transaccionId)).thenReturn(List.of(original));

        assertThatThrownBy(() -> servicio.registrar(comandoPrecio(new BigDecimal("6.00"), false, Set.of())))
                .isInstanceOf(TransaccionVentaReutilizadaException.class);
        verify(ventas, never()).guardarTodas(anyList());
        verify(stock, never()).descontar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void exigeScopeEspecialParaForzarVenta() {
        assertThatThrownBy(() -> servicio.registrar(comando(true, Set.of())))
                .isInstanceOf(OperacionVentaProhibidaException.class);
        verify(ventas, never()).buscarPorTransaccion(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scopeEspecialPermiteVentaForzadaAuditada() {
        prepararTiendaYVariante();
        when(stock.descontar(eq(almacenId), eq(varianteId), eq(2),
                org.mockito.ArgumentMatchers.any(UUID.class), eq(true))).thenReturn(true);
        when(ventas.guardarTodas(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.registrar(comando(true, Set.of(VentaService.SCOPE_FORZAR_SIN_STOCK)));

        assertThat(resultado.lineas()).singleElement()
                .satisfies(venta -> assertThat(venta.forzada()).isTrue());
        verify(stock).descontar(almacenId, varianteId, 2, resultado.lineas().get(0).id(), true);
    }

    @Test
    void stockInsuficienteRechazaLaVentaSinGuardarLineas() {
        prepararTiendaYVariante();
        when(stock.descontar(eq(almacenId), eq(varianteId), eq(2),
                org.mockito.ArgumentMatchers.any(UUID.class), eq(false))).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(comando(false, Set.of())))
                .isInstanceOf(SinStockException.class);
        verify(ventas, never()).guardarTodas(anyList());
    }

    private void prepararTiendaYVariante() {
        when(ventas.buscarPorTransaccion(transaccionId)).thenReturn(List.of());
        when(tiendas.buscar(tiendaId)).thenReturn(Optional.of(new TiendaVentaPort.TiendaVenta(tiendaId, true, almacenId)));
        when(variantes.buscarIdPorSku("SKU-1")).thenReturn(Optional.of(varianteId));
        when(stock.descontar(eq(almacenId), eq(varianteId), eq(2),
                org.mockito.ArgumentMatchers.any(UUID.class), eq(false))).thenReturn(true);
    }

    private Comando comando(boolean forzar, Set<String> scopes) {
        return comandoPrecio(new BigDecimal("5.25"), forzar, scopes);
    }

    private Comando comandoPrecio(BigDecimal precio, boolean forzar, Set<String> scopes) {
        UsuarioActual vendedor = new UsuarioActual(vendedorId, scopes, Set.of(tiendaId));
        return new Comando(vendedor, transaccionId, tiendaId, null, forzar,
                List.of(new Item("SKU-1", 2, precio)));
    }

    private Venta venta(boolean forzar) {
        return Venta.registrarPorApi(transaccionId, varianteId, tiendaId, 2, new BigDecimal("5.25"),
                "FISICO", vendedorId, forzar, AHORA);
    }
}
