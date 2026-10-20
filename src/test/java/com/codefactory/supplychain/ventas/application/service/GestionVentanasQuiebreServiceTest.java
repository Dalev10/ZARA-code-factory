package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase.ExistenciasCambiadas;
import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU-45: apertura y cierre de ventanas por cruce del saldo del Almacén (D-46, D-47, D-48, D-50). */
class GestionVentanasQuiebreServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    private final UUID tiendaId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private VentanaQuiebreRepositoryPort ventanas;
    private GestionVentanasQuiebreService servicio;

    @BeforeEach
    void setUp() {
        ventanas = mock(VentanaQuiebreRepositoryPort.class);
        servicio = new GestionVentanasQuiebreService(ventanas);
        when(ventanas.buscarAbierta(tiendaId, varianteId)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("El Almacén cruza de 1 a 0: abre una ventana con la fecha del movimiento")
    void abreAlCruzarACero() {
        servicio.alCambiarExistencias(cambio(false, 1, 0));

        verify(ventanas).abrirSiAusente(org.mockito.ArgumentMatchers.argThat(v ->
                v.getTiendaId().equals(tiendaId) && v.getFechaInicio().equals(AHORA) && v.estaAbierta()));
    }

    @Test
    @DisplayName("Una carga en 0 o un ajuste entre saldos <= 0 no cruzan y no abren")
    void sinCruceNoAbre() {
        servicio.alCambiarExistencias(cambio(false, 0, 0));
        servicio.alCambiarExistencias(cambio(false, -2, -1));

        verify(ventanas, never()).abrirSiAusente(any());
    }

    @Test
    @DisplayName("Un evento repetido con ventana ya abierta no abre otra")
    void eventoRepetidoNoAbreDos() {
        when(ventanas.buscarAbierta(tiendaId, varianteId))
                .thenReturn(Optional.of(VentanaQuiebre.abrir(tiendaId, varianteId, AHORA)));

        servicio.alCambiarExistencias(cambio(false, 1, 0));

        verify(ventanas, never()).abrirSiAusente(any());
    }

    @Test
    @DisplayName("El saldo vuelve a ser positivo: cierra con REPOSICION")
    void cierraConReposicion() {
        VentanaQuiebre abierta = VentanaQuiebre.abrir(tiendaId, varianteId, AHORA.minusSeconds(3600));
        when(ventanas.buscarAbierta(tiendaId, varianteId)).thenReturn(Optional.of(abierta));

        servicio.alCambiarExistencias(cambio(false, 0, 4));

        assertThat(abierta.getMotivoCierre()).isEqualTo(MotivoCierreVentana.REPOSICION);
        assertThat(abierta.getFechaFin()).isEqualTo(AHORA);
        verify(ventanas).guardar(abierta);
    }

    @Test
    @DisplayName("Una venta forzada cierra con VENTA_FORZADA y nunca abre")
    void ventaForzadaCierraYNoAbre() {
        servicio.alCambiarExistencias(cambio(true, 1, -2));
        verify(ventanas, never()).abrirSiAusente(any());

        VentanaQuiebre abierta = VentanaQuiebre.abrir(tiendaId, varianteId, AHORA.minusSeconds(60));
        when(ventanas.buscarAbierta(tiendaId, varianteId)).thenReturn(Optional.of(abierta));
        servicio.alCambiarExistencias(cambio(true, 0, -1));

        assertThat(abierta.getMotivoCierre()).isEqualTo(MotivoCierreVentana.VENTA_FORZADA);
    }

    @Test
    @DisplayName("Un movimiento fuera de un Almacén se ignora")
    void ignoraNodosQueNoSonAlmacen() {
        servicio.alCambiarExistencias(new ExistenciasCambiadas(null, varianteId, false, 1, 0, AHORA));

        verifyNoInteractions(ventanas);
    }

    @Test
    @DisplayName("Al desactivar la tienda se cierran sus ventanas con TIENDA_DESACTIVADA")
    void cierraLasVentanasDeUnaTiendaDesactivada() {
        VentanaQuiebre a = VentanaQuiebre.abrir(tiendaId, varianteId, AHORA.minusSeconds(60));
        VentanaQuiebre b = VentanaQuiebre.abrir(tiendaId, UUID.randomUUID(), AHORA.minusSeconds(60));
        when(ventanas.listarAbiertasPorTienda(tiendaId)).thenReturn(List.of(a, b));

        int cerradas = servicio.alDesactivarTienda(tiendaId, AHORA);

        assertThat(cerradas).isEqualTo(2);
        assertThat(List.of(a, b)).allMatch(v -> v.getMotivoCierre() == MotivoCierreVentana.TIENDA_DESACTIVADA);
    }

    private ExistenciasCambiadas cambio(boolean forzado, int anterior, int actual) {
        return new ExistenciasCambiadas(tiendaId, varianteId, forzado, anterior, actual, AHORA);
    }
}
