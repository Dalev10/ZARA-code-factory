package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.CantidadRecibidaExcedeDespachoException;
import com.codefactory.supplychain.logistica.domain.exception.RecepcionInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-49: invariantes de la recepción de una parada (D-90, D-91). */
class RecepcionTest {

    private final UUID paradaId = UUID.randomUUID();
    private final UUID cicloId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-11-20T15:00:00Z");

    @Test
    void recepcionTotalNoDejaDiscrepancia() {
        Recepcion recepcion = Recepcion.registrar(paradaId, cicloId, 10, 10, usuarioId, ahora);

        assertThat(recepcion.getId()).isNotNull();
        assertThat(recepcion.getCantidadRecibida()).isEqualTo(10);
        assertThat(recepcion.getDiscrepancia()).isZero();
        assertThat(recepcion.tieneDiscrepancia()).isFalse();
        assertThat(recepcion.recibioMercancia()).isTrue();
        assertThat(recepcion.getViajeParadaId()).isEqualTo(paradaId);
        assertThat(recepcion.getCicloDespachoId()).isEqualTo(cicloId);
        assertThat(recepcion.getRegistradoPor()).isEqualTo(usuarioId);
        assertThat(recepcion.getFechaRecepcion()).isEqualTo(ahora);
    }

    @Test
    void recepcionParcialRegistraLaPerdidaEnTransito() {
        Recepcion recepcion = Recepcion.registrar(paradaId, cicloId, 10, 7, usuarioId, ahora);

        assertThat(recepcion.getDiscrepancia()).isEqualTo(3);
        assertThat(recepcion.tieneDiscrepancia()).isTrue();
        assertThat(recepcion.cantidadDespachada()).isEqualTo(10);
    }

    @Test
    void recibirCeroEsValidoYTodoQuedaComoDiscrepancia() {
        Recepcion recepcion = Recepcion.registrar(paradaId, cicloId, 10, 0, usuarioId, ahora);

        assertThat(recepcion.recibioMercancia()).isFalse();
        assertThat(recepcion.getDiscrepancia()).isEqualTo(10);
    }

    @Test
    void noSePuedeRecibirMasDeLoDespachado() {
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, cicloId, 10, 11, usuarioId, ahora))
                .isInstanceOf(CantidadRecibidaExcedeDespachoException.class)
                .hasMessageContaining("11")
                .hasMessageContaining("10");
    }

    @Test
    void lasCantidadesNegativasSeRechazan() {
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, cicloId, 10, -1, usuarioId, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, cicloId, -1, -2, usuarioId, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
    }

    @Test
    void exigeParadaCicloUsuarioYFecha() {
        assertThatThrownBy(() -> Recepcion.registrar(null, cicloId, 10, 10, usuarioId, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, null, 10, 10, usuarioId, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, cicloId, 10, 10, null, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
        assertThatThrownBy(() -> Recepcion.registrar(paradaId, cicloId, 10, 10, usuarioId, null))
                .isInstanceOf(RecepcionInvalidaException.class);
    }

    @Test
    void reconstruirConservaLosDatosYValidaLasInvariantes() {
        UUID id = UUID.randomUUID();
        Recepcion recepcion = Recepcion.reconstruir(id, paradaId, cicloId, 7, 3, usuarioId, ahora);

        assertThat(recepcion.getId()).isEqualTo(id);
        assertThat(recepcion.cantidadDespachada()).isEqualTo(10);
        assertThat(recepcion).isEqualTo(Recepcion.reconstruir(id, paradaId, cicloId, 0, 10, usuarioId, ahora));
        assertThatThrownBy(() -> Recepcion.reconstruir(id, paradaId, cicloId, 7, -1, usuarioId, ahora))
                .isInstanceOf(RecepcionInvalidaException.class);
    }
}
