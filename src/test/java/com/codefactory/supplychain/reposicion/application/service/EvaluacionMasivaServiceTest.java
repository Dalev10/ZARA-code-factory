package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
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

class EvaluacionMasivaServiceTest {

    private TraspasoAutomaticoUseCase traspasoAutomatico;
    private ConsultarTiendasActivasPort tiendasActivas;
    private ConsultarExistenciasPort variantesPorTienda;
    private EvaluacionMasivaService servicio;

    private static final UUID TIENDA_A = UUID.randomUUID();
    private static final UUID TIENDA_B = UUID.randomUUID();
    private static final UUID VARIANTE_1 = UUID.randomUUID();
    private static final UUID VARIANTE_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        traspasoAutomatico = mock(TraspasoAutomaticoUseCase.class);
        tiendasActivas = mock(ConsultarTiendasActivasPort.class);
        variantesPorTienda = mock(ConsultarExistenciasPort.class);
        when(tiendasActivas.existe(any())).thenReturn(true);
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

        List<TraspasoInterno> resultado = servicio.evaluarTodas();

        assertThat(resultado).containsExactly(t1, t2);
    }

    @Test
    void evaluarTodasConSinTiendasDevuelveListaVacia() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of());

        assertThat(servicio.evaluarTodas()).isEmpty();
        verify(variantesPorTienda, never()).variantesConInventario(any());
    }

    @Test
    void evaluarTiendasSinVariantesNoGeneraTraspasos() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of());

        assertThat(servicio.evaluarTodas()).isEmpty();
        verify(traspasoAutomatico, never()).evaluar(any(), any());
    }

    @Test
    void evaluarTiendasDondeNingunVarianteDisparaDevuelveVacio() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        when(traspasoAutomatico.evaluar(any(), any())).thenReturn(Optional.empty());

        assertThat(servicio.evaluarTodas()).isEmpty();
    }

    @Test
    void evaluarTiendaEspecificaIgnoraLasOtras() {
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));

        List<TraspasoInterno> resultado = servicio.evaluarTienda(TIENDA_A);

        assertThat(resultado).containsExactly(t1);
        verify(tiendasActivas, never()).listarTiendasActivas();
    }

    @Test
    void evaluarTiendaInexistenteLanzaNoEncontrado() {
        when(tiendasActivas.existe(TIENDA_A)).thenReturn(false);

        assertThatThrownBy(() -> servicio.evaluarTienda(TIENDA_A))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class)
                .hasMessageContaining(TIENDA_A.toString());
        verify(variantesPorTienda, never()).variantesConInventario(any());
    }

    @Test
    void evaluarTodasSoloAcumulaLosTraspasosQueSeEjecutaron() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        TraspasoInterno t1 = traspasoFake(TIENDA_A, VARIANTE_1);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenReturn(Optional.of(t1));
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_2)).thenReturn(Optional.empty());

        assertThat(servicio.evaluarTodas()).containsExactly(t1);
    }

    @Test
    void unaVarianteQueFallaNoDetieneLasDemas() {
        when(tiendasActivas.listarTiendasActivas()).thenReturn(List.of(TIENDA_A));
        when(variantesPorTienda.variantesConInventario(TIENDA_A)).thenReturn(List.of(VARIANTE_1, VARIANTE_2));
        TraspasoInterno t2 = traspasoFake(TIENDA_A, VARIANTE_2);
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_1)).thenThrow(new IllegalStateException("falla"));
        when(traspasoAutomatico.evaluar(TIENDA_A, VARIANTE_2)).thenReturn(Optional.of(t2));

        assertThat(servicio.evaluarTodas()).containsExactly(t2);
    }
}
