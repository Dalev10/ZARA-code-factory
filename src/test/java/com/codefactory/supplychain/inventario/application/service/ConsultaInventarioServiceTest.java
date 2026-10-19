package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.ConsultaKardexComando;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultaInventarioServiceTest {

    private AccesoNodoInventario acceso;
    private InventarioRepositoryPort inventario;
    private MovimientoInventarioRepositoryPort movimientos;
    private ConsultaInventarioService servicio;

    private final UUID nodoId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID tiendaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        acceso = mock(AccesoNodoInventario.class);
        inventario = mock(InventarioRepositoryPort.class);
        movimientos = mock(MovimientoInventarioRepositoryPort.class);
        servicio = new ConsultaInventarioService(acceso, inventario, movimientos);
    }

    @Test
    void stockDeUnNodoAjenoFallaSinConsultarInventario() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of(), Set.of());
        when(acceso.exigirVisibilidad(nodoId, usuario)).thenThrow(new NodoNoEncontradoException(nodoId));

        assertThatThrownBy(() -> servicio.stockDeNodo(nodoId, usuario, PageRequest.of(0, 20)))
                .isInstanceOf(NodoNoEncontradoException.class);
        verifyNoInteractions(inventario);
    }

    @Test
    void stockDeVarianteSinTiendasNoConsultaNada() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("inventario:consultar"), Set.of());

        assertThat(servicio.stockDeVariante(varianteId, usuario)).isEmpty();
        verifyNoInteractions(inventario);
    }

    @Test
    void stockDeVarianteDeUsuarioConTiendasFiltraPorEllas() {
        UsuarioActual usuario = new UsuarioActual(
                UUID.randomUUID(), Set.of("inventario:consultar"), Set.of(tiendaId));
        when(inventario.buscarPorVarianteEnTiendas(varianteId, Set.of(tiendaId))).thenReturn(List.of());

        servicio.stockDeVariante(varianteId, usuario);

        verify(inventario).buscarPorVarianteEnTiendas(varianteId, Set.of(tiendaId));
        verify(inventario, never()).buscarPorVariante(any());
    }

    @Test
    void stockDeVarianteConAccesoGlobalTraeTodosLosNodos() {
        UsuarioActual usuario = new UsuarioActual(
                UUID.randomUUID(),
                Set.of("inventario:consultar", UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS),
                Set.of());
        when(inventario.buscarPorVariante(varianteId)).thenReturn(List.of());

        servicio.stockDeVariante(varianteId, usuario);

        verify(inventario).buscarPorVariante(varianteId);
        verify(inventario, never()).buscarPorVarianteEnTiendas(any(), any());
    }

    @Test
    void stockDeVarianteDelOperadorDeCdIncluyeLosCds() {
        UsuarioActual usuario = new UsuarioActual(
                UUID.randomUUID(), Set.of("inventario:consultar", "inventario-cd:administrar"), Set.of());
        when(inventario.buscarPorVarianteEnCds(varianteId)).thenReturn(List.of());

        servicio.stockDeVariante(varianteId, usuario);

        verify(inventario).buscarPorVarianteEnCds(varianteId);
        verify(inventario, never()).buscarPorVarianteEnTiendas(any(), any());
    }

    @Test
    void kardexSinFechasNoAcotaElRango() {
        UsuarioActual usuario = new UsuarioActual(
                UUID.randomUUID(),
                Set.of("inventario:consultar", UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS),
                Set.of());
        var pagina = PageRequest.of(0, 20);
        var comando = new ConsultaKardexComando(nodoId, null, null, null, usuario);

        servicio.kardex(comando, pagina);

        verify(movimientos).buscarKardex(
                eq(nodoId), eq(null), eq(Instant.EPOCH), any(Instant.class), eq(pagina));
    }
}
