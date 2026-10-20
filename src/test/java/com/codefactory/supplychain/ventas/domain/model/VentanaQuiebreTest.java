package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.VentanaInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VentanaQuiebreTest {

    private final UUID tiendaId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final Instant inicio = Instant.parse("2026-10-18T10:00:00Z");

    @Test
    void abreYReconstituyeVentana() {
        VentanaQuiebre ventana = VentanaQuiebre.abrir(tiendaId, varianteId, inicio);

        assertThat(ventana.getId()).isNotNull();
        assertThat(ventana.getTiendaId()).isEqualTo(tiendaId);
        assertThat(ventana.getVarianteId()).isEqualTo(varianteId);
        assertThat(ventana.getFechaInicio()).isEqualTo(inicio);
        assertThat(ventana.getFechaFin()).isNull();
        assertThat(ventana.getMotivoCierre()).isNull();
        assertThat(ventana.estaAbierta()).isTrue();

        VentanaQuiebre reconstituida = VentanaQuiebre.reconstituir(
                ventana.getId(), tiendaId, varianteId, inicio, null, null);
        assertThat(reconstituida.estaAbierta()).isTrue();
    }

    @Test
    void exigeLosDatosParaAbrir() {
        assertThatThrownBy(() -> VentanaQuiebre.abrir(null, varianteId, inicio))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> VentanaQuiebre.abrir(tiendaId, null, inicio))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> VentanaQuiebre.abrir(tiendaId, varianteId, null))
                .isInstanceOf(VentanaInvalidaException.class);
    }

    @Test
    void cierraYNormalizaFechaAnteriorAlInicio() {
        VentanaQuiebre ventana = VentanaQuiebre.abrir(tiendaId, varianteId, inicio);
        ventana.cerrar(inicio.minusSeconds(10), MotivoCierreVentana.REPOSICION);

        assertThat(ventana.estaAbierta()).isFalse();
        assertThat(ventana.getFechaFin()).isEqualTo(inicio);
        assertThat(ventana.getMotivoCierre()).isEqualTo(MotivoCierreVentana.REPOSICION);
        assertThatThrownBy(() -> ventana.cerrar(inicio, MotivoCierreVentana.REPOSICION))
                .isInstanceOf(VentanaInvalidaException.class);
    }

    @Test
    void exigeFechaYMotivoParaCerrar() {
        VentanaQuiebre ventana = VentanaQuiebre.abrir(tiendaId, varianteId, inicio);
        assertThatThrownBy(() -> ventana.cerrar(null, MotivoCierreVentana.REPOSICION))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> ventana.cerrar(inicio, null))
                .isInstanceOf(VentanaInvalidaException.class);
    }

    @Test
    void corrigeCierreSoloConFechaValidaYMotivo() {
        VentanaQuiebre ventana = VentanaQuiebre.reconstituir(UUID.randomUUID(), tiendaId, varianteId,
                inicio, inicio.plusSeconds(5), MotivoCierreVentana.REPOSICION);
        Instant correccion = inicio.plusSeconds(20);
        ventana.corregirCierre(correccion, MotivoCierreVentana.VENTA_FORZADA);

        assertThat(ventana.getFechaFin()).isEqualTo(correccion);
        assertThat(ventana.getMotivoCierre()).isEqualTo(MotivoCierreVentana.VENTA_FORZADA);
        assertThatThrownBy(() -> ventana.corregirCierre(null, MotivoCierreVentana.REPOSICION))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> ventana.corregirCierre(inicio.minusSeconds(1), MotivoCierreVentana.REPOSICION))
                .isInstanceOf(VentanaInvalidaException.class);
        assertThatThrownBy(() -> ventana.corregirCierre(inicio, null))
                .isInstanceOf(VentanaInvalidaException.class);
    }
}
