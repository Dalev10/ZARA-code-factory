package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class EvaluacionMasivaServiceTest {

    private TraspasoAutomaticoUseCase traspasoAutomatico;
    private ConsultarTiendasActivasPort tiendasActivas;
    private ConsultarExistenciasPort variantesPorTienda;
    private EvaluacionMasivaService servicio;

    private static final UUID TIENDA_A = UUID.randomUUID();
    private static final UUID TIENDA_B = UUID.randomUUID();
    private static final UUID VARIANTE_1 = UUID.randomUUID();
    private static final UUID VARIANTE_2 = UUID.randomUUID();
    private static final UsuarioActual GLOBAL = new UsuarioActual(UUID.randomUUID(),
            Set.of("reposicion:administrar", UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS), Set.of());
    private static final UsuarioActual SOLO_TIENDA_A = new UsuarioActual(UUID.randomUUID(),
            Set.of("reposicion:administrar"), Set.of(TIENDA_A));

    @BeforeEach
    void setUp() {
        traspasoAutomatico = mock(TraspasoAutomaticoUseCase.class);
        tiendasActivas = mock(ConsultarTiendasActivasPort.class);
        variantesPorTienda = mock(ConsultarExistenciasPort.class);
        servicio = new EvaluacionMasivaService(traspasoAutomatico, tiendasActivas, variantesPorTienda);
    }

    private TraspasoInterno traspasoFake(UUID tiendaId, UUID varianteId) {
        return TraspasoInterno.reconstruir(UUID.randomUUID(), varianteId, tiendaId, 3,
                OrigenTraspaso.AUTOMATICO, null, Instant.now());
    }

    @Test
    void evaluarTodasRecorreCadaTiendaActivaYSusVariantes() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A, TIENDA_B));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1));
        when(variantesPorTienda.variantesConInventario(TIENDA_B)).thenReturn(List.of(VARIANTE_2));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        TraspasoInterno t2 = traspasoFake(TIENDA_B, VARIANTE_2);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));
        when(traspasoAutomatico.evaluar(TIENDA_B, VARIANTE_2)).thenReturn(Optional.of(t2));

        List<TraspasoInterno> resultado = servicio.evaluarTodas(GLOBAL);

        assertThat(resultado).containsExactly(t1, t2);
    }

    @Test
    void evaluarTodasConSinTiendasDevuelveListaVacia() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of());

        assertThat(servicio.evaluarTodas(GLOBAL)).isEmpty();
        verify(variantesPorTienda, never()).variantesConInventario(any());
    }

    @Test
    void evaluarTiendasSinVariantesNoGeneraTraspasos() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of());

        assertThat(servicio.evaluarTodas(GLOBAL)).isEmpty();
        verify(traspasoAutomatico, never()).evaluar(any(), any());
    }

    @Test
    void evaluarTiendasDondeNingunVarianteDisparaDevuelveVacio() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        when(traspasoAutomatico.evaluar(any(), any())).thenReturn(Optional.empty());

        assertThat(servicio.evaluarTodas(GLOBAL)).isEmpty();
    }

    @Test
    void evaluarTiendaEspecificaIgnoraLasOtras() {
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));

        List<TraspasoInterno> resultado = servicio.evaluarTienda(GLOBAL, TIENDA_A);

        assertThat(resultado).containsExactly(t1);
        verify(tiendasActivas, never()).listarTiendasActivas();
    }

    @Test
    void evaluarTodasSoloAcumulaLosTraspasosQueSeEjecutaron() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_2)).thenReturn(Optional.empty());

        assertThat(servicio.evaluarTodas(GLOBAL)).containsExactly(t1);
    }

    @Test
    void unaVarianteQueFallaNoDetieneLasDemas() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        TraspasoInterno t2 = traspasoFake(TIENDA_A, VARIANTE_2);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenThrow(new IllegalStateException("falla"));
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_2)).thenReturn(Optional.of(t2));

        assertThat(servicio.evaluarTodas(GLOBAL)).containsExactly(t2);
    }

    @Test
    void evaluarTodasSoloRecorreLasTiendasDelUsuario() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A, TIENDA_B));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));

        assertThat(servicio.evaluarTodas(SOLO_TIENDA_A)).containsExactly(t1);
        verify(variantesPorTienda, never()).variantesConInventario(TIENDA_B);
    }

    @Test
    void evaluarUnaTiendaAjenaEsProhibidoYNoEvaluaNada() {
        assertThatThrownBy(() -> servicio.evaluarTienda(SOLO_TIENDA_A, TIENDA_B))
                .isInstanceOf(TiendaNoPermitidaException.class);
        verify(variantesPorTienda, never()).variantesConInventario(any());
        verify(traspasoAutomatico, never()).evaluar(any(), any());
    }
}
