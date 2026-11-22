package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.OrdenInvalidaException;
import com.codefactory.supplychain.reposicion.domain.exception.TransicionOrdenInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrdenReabastecimiento: transiciones del ciclo")
class OrdenReabastecimientoTransicionesTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T10:00:00Z");

    private static OrdenReabastecimiento enEstado(EstadoOrden estado, int s, int pendiente, int despachada) {
        UUID cd = estado == EstadoOrden.GENERADA ? null : UUID.randomUUID();
        return OrdenReabastecimiento.reconstruir(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                cd, null, s, pendiente, despachada, estado, AHORA, 0L, List.of(), null);
    }

    // ---------------------------------------------------------------- válidas

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = {"APROBADA", "REABIERTA"})
    @DisplayName("iniciarDespacho: APROBADA o REABIERTA → EN_DESPACHO")
    void iniciarDespachoValido(EstadoOrden origen) {
        var orden = enEstado(origen, 10, 10, 0);
        orden.iniciarDespacho();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_DESPACHO);
    }

    @Test
    @DisplayName("confirmarDespacho: EN_DESPACHO → DESPACHADA y acumula ΣD")
    void confirmarDespachoValido() {
        var orden = enEstado(EstadoOrden.EN_DESPACHO, 10, 10, 0);
        orden.confirmarDespacho(6);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.DESPACHADA);
        assertThat(orden.getCantidadDespachadaAcumulada()).isEqualTo(6);
        assertThat(orden.reservaRestante()).isEqualTo(4);
        assertThat(orden.getCantidadPendiente()).isEqualTo(10);
    }

    @Test
    @DisplayName("marcarEnTransito: DESPACHADA → EN_TRANSITO")
    void marcarEnTransitoValido() {
        var orden = enEstado(EstadoOrden.DESPACHADA, 10, 10, 10);
        orden.marcarEnTransito();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_TRANSITO);
    }

    @Test
    @DisplayName("registrarRecepcion total: pendiente 0 → CERRADA")
    void recepcionTotal() {
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 10, 10);
        assertThat(orden.registrarRecepcion(10)).isEqualTo(EstadoOrden.CERRADA);
        assertThat(orden.getCantidadPendiente()).isZero();
    }

    @Test
    @DisplayName("registrarRecepcion parcial: queda pendiente → REABIERTA y P = S − ΣR")
    void recepcionParcial() {
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 10, 10);
        assertThat(orden.registrarRecepcion(7)).isEqualTo(EstadoOrden.REABIERTA);
        assertThat(orden.getCantidadPendiente()).isEqualTo(3);
        assertThat(orden.cantidadRecibidaAcumulada()).isEqualTo(7);
        // Los 3 perdidos no cuentan: no se puede recibir más que el pendiente.
        assertThat(orden.maximoRecibible()).isEqualTo(3);
    }

    @Test
    @DisplayName("despacho parcial recibido completo: queda pendiente por despachar → REABIERTA")
    void despachoParcialRecibidoCompleto() {
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 10, 6);
        assertThat(orden.registrarRecepcion(6)).isEqualTo(EstadoOrden.REABIERTA);
        assertThat(orden.getCantidadPendiente()).isEqualTo(4);
        assertThat(orden.reservaRestante()).isEqualTo(4);
    }

    @Test
    @DisplayName("recepción de 0 (todo perdido): REABIERTA con el pendiente intacto")
    void recepcionCero() {
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 10, 10);
        assertThat(orden.registrarRecepcion(0)).isEqualTo(EstadoOrden.REABIERTA);
        assertThat(orden.getCantidadPendiente()).isEqualTo(10);
    }

    // -------------------------------------------------------------- inválidas

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = {"APROBADA", "REABIERTA"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("iniciarDespacho en otro estado: transición inválida")
    void iniciarDespachoInvalido(EstadoOrden estado) {
        var orden = enEstado(estado, 10, 10, 0);
        assertThatThrownBy(orden::iniciarDespacho).isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThat(orden.getEstado()).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = "EN_DESPACHO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("confirmarDespacho fuera de EN_DESPACHO: transición inválida")
    void confirmarDespachoInvalido(EstadoOrden estado) {
        var orden = enEstado(estado, 10, 10, 0);
        assertThatThrownBy(() -> orden.confirmarDespacho(5)).isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThat(orden.getCantidadDespachadaAcumulada()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = "DESPACHADA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("marcarEnTransito fuera de DESPACHADA: transición inválida")
    void marcarEnTransitoInvalido(EstadoOrden estado) {
        var orden = enEstado(estado, 10, 10, 10);
        assertThatThrownBy(orden::marcarEnTransito).isInstanceOf(TransicionOrdenInvalidaException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = "EN_TRANSITO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("registrarRecepcion fuera de EN_TRANSITO: transición inválida")
    void recepcionInvalida(EstadoOrden estado) {
        var orden = enEstado(estado, 10, 10, 10);
        assertThatThrownBy(() -> orden.registrarRecepcion(5)).isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThat(orden.getCantidadPendiente()).isEqualTo(10);
    }

    @Test
    @DisplayName("confirmarDespacho con cantidad ≤ 0 o mayor que lo solicitado del ciclo: inválida")
    void cantidadDespachadaInvalida() {
        var orden = enEstado(EstadoOrden.EN_DESPACHO, 10, 4, 6);
        assertThatThrownBy(() -> orden.confirmarDespacho(0)).isInstanceOf(OrdenInvalidaException.class);
        assertThatThrownBy(() -> orden.confirmarDespacho(5)).isInstanceOf(OrdenInvalidaException.class);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_DESPACHO);
    }

    @Test
    @DisplayName("tras una pérdida en tránsito, el ciclo siguiente no puede recibir más que el pendiente")
    void perdidaNoInflaLoRecibible() {
        // S=10, ΣD=13 (10 + 3), ΣR=7: P=3. ΣD − ΣR = 6 incluye 3 perdidos.
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 3, 13);
        assertThatThrownBy(() -> orden.registrarRecepcion(4)).isInstanceOf(OrdenInvalidaException.class);
        assertThat(orden.registrarRecepcion(3)).isEqualTo(EstadoOrden.CERRADA);
    }

    @Test
    @DisplayName("registrarRecepcion negativa o mayor que lo despachado del ciclo: inválida")
    void cantidadRecibidaInvalida() {
        var orden = enEstado(EstadoOrden.EN_TRANSITO, 10, 10, 6);
        assertThatThrownBy(() -> orden.registrarRecepcion(-1)).isInstanceOf(OrdenInvalidaException.class);
        assertThatThrownBy(() -> orden.registrarRecepcion(7)).isInstanceOf(OrdenInvalidaException.class);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_TRANSITO);
    }
}
