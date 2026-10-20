package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.ConsultarVentasUseCase.Filtros;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.VentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarVentasServiceTest {

    private final UUID tiendaId = UUID.randomUUID();
    private final UUID anuladaTiendaId = UUID.randomUUID();
    private final UUID ventaId = UUID.randomUUID();
    private VentaRepositoryPort ventas;
    private ConsultarVentasService servicio;

    @BeforeEach
    void setUp() {
        ventas = mock(VentaRepositoryPort.class);
        servicio = new ConsultarVentasService(ventas);
    }

    @Test
    void aplicaElFiltroDeTiendasPermitidasAlListado() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("ventas:consultar"), Set.of(tiendaId));
        Filtros filtros = new Filtros(null, null, null, null, null, null);
        when(ventas.buscar(any(), any())).thenReturn(new PageImpl<>(List.of()));

        servicio.consultar(usuario, filtros, PageRequest.of(0, 20));

        verify(ventas).buscar(org.mockito.ArgumentMatchers.argThat(f -> f.tiendasPermitidas().contains(tiendaId)),
                any());
    }

    @Test
    void filtrarTiendaAjenaDevuelvePaginaVaciaSinConsultarDatos() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("ventas:consultar"), Set.of(tiendaId));

        var page = servicio.consultar(usuario,
                new Filtros(anuladaTiendaId, null, null, null, null, null), PageRequest.of(0, 20));

        assertThat(page).isEmpty();
        verify(ventas, never()).buscar(any(), any());
    }

    @Test
    void detalleDeTiendaAjenaRespondeComoNoEncontrado() {
        Venta venta = Venta.registrarPorApi(UUID.randomUUID(), UUID.randomUUID(), anuladaTiendaId, 1,
                BigDecimal.ONE, "FISICO", UUID.randomUUID(), false, Instant.now());
        when(ventas.buscarPorId(ventaId)).thenReturn(Optional.of(venta));
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("ventas:consultar"), Set.of(tiendaId));

        assertThatThrownBy(() -> servicio.obtener(usuario, ventaId)).isInstanceOf(VentaNoEncontradaException.class);
    }
}
