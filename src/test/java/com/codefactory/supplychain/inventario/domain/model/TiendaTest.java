package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.TiendaInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiendaTest {

    @Test
    void crearAsignaIdEstadoActivaYFechas() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");

        assertThat(tienda.getId()).isNotNull();
        assertThat(tienda.getNombre()).isEqualTo("Tienda Centro");
        assertThat(tienda.getEstado()).isEqualTo(EstadoTienda.ACTIVA);
        assertThat(tienda.estaActiva()).isTrue();
        assertThat(tienda.getCreadoEn()).isNotNull();
        assertThat(tienda.getActualizadoEn()).isEqualTo(tienda.getCreadoEn());
    }

    @Test
    void rechazaNombreVacio() {
        assertThatThrownBy(() -> Tienda.crear("  ", "Bogotá")).isInstanceOf(TiendaInvalidaException.class);
    }

    @Test
    void rechazaNombreNulo() {
        assertThatThrownBy(() -> Tienda.crear(null, "Bogotá")).isInstanceOf(TiendaInvalidaException.class);
    }

    @Test
    void rechazaNombreDemasiadoLargo() {
        assertThatThrownBy(() -> Tienda.crear("a".repeat(151), "Bogotá"))
                .isInstanceOf(TiendaInvalidaException.class);
    }

    @Test
    void actualizarConservaIdEstadoYCreadoEnPeroCambiaNombreUbicacionYActualizadoEn() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");

        Tienda actualizada = tienda.actualizar("Tienda Centro Renovada", "Medellín");

        assertThat(actualizada.getId()).isEqualTo(tienda.getId());
        assertThat(actualizada.getEstado()).isEqualTo(EstadoTienda.ACTIVA);
        assertThat(actualizada.getCreadoEn()).isEqualTo(tienda.getCreadoEn());
        assertThat(actualizada.getNombre()).isEqualTo("Tienda Centro Renovada");
        assertThat(actualizada.getUbicacion()).isEqualTo("Medellín");
    }

    @Test
    void desactivarCambiaElEstadoAInactivaSinTocarElResto() {
        Tienda tienda = Tienda.crear("Tienda Centro", "Bogotá");

        Tienda desactivada = tienda.desactivar();

        assertThat(desactivada.getEstado()).isEqualTo(EstadoTienda.INACTIVA);
        assertThat(desactivada.estaActiva()).isFalse();
        assertThat(desactivada.getId()).isEqualTo(tienda.getId());
        assertThat(desactivada.getNombre()).isEqualTo(tienda.getNombre());
    }

    @Test
    void reconstruirPermiteRehidratarUnaTiendaExistente() {
        UUID id = UUID.randomUUID();
        Instant creadoEn = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizadoEn = Instant.parse("2026-02-01T00:00:00Z");

        Tienda tienda = Tienda.reconstruir(id, "Tienda Sur", "Cali", EstadoTienda.INACTIVA, creadoEn, actualizadoEn);

        assertThat(tienda.getId()).isEqualTo(id);
        assertThat(tienda.getEstado()).isEqualTo(EstadoTienda.INACTIVA);
        assertThat(tienda.getCreadoEn()).isEqualTo(creadoEn);
        assertThat(tienda.getActualizadoEn()).isEqualTo(actualizadoEn);
    }
}
