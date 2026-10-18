package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudYaAnuladaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitudNoSatisfechaTest {

    private final UUID tiendaId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-10-18T10:00:00Z");

    @Test
    void registraSolicitudActivaConStockObservado() {
        SolicitudNoSatisfecha solicitud = SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 3, 7, ahora, usuarioId, "req-1");

        assertThat(solicitud.getId()).isNotNull();
        assertThat(solicitud.getTiendaId()).isEqualTo(tiendaId);
        assertThat(solicitud.getVarianteId()).isEqualTo(varianteId);
        assertThat(solicitud.getCantidad()).isEqualTo(3);
        assertThat(solicitud.getStockSistema()).isEqualTo(7);
        assertThat(solicitud.getRegistradaEn()).isEqualTo(ahora);
        assertThat(solicitud.getRegistradoPor()).isEqualTo(usuarioId);
        assertThat(solicitud.getClaveIdempotencia()).isEqualTo("req-1");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ACTIVA);
        assertThat(solicitud.getAnuladaPor()).isNull();
        assertThat(solicitud.getAnuladaEn()).isNull();
        assertThat(solicitud.esRepeticionDe(usuarioId, tiendaId, varianteId, 3)).isTrue();
        assertThat(solicitud.esRepeticionDe(UUID.randomUUID(), tiendaId, varianteId, 3)).isFalse();
        assertThat(solicitud.esRepeticionDe(usuarioId, UUID.randomUUID(), varianteId, 3)).isFalse();
        assertThat(solicitud.esRepeticionDe(usuarioId, tiendaId, UUID.randomUUID(), 3)).isFalse();
        assertThat(solicitud.esRepeticionDe(usuarioId, tiendaId, varianteId, 4)).isFalse();
    }

    @Test
    void aceptaStockDesconocidoYClaveEnLimite() {
        String clave = "x".repeat(SolicitudNoSatisfecha.LONGITUD_MAXIMA_CLAVE);
        SolicitudNoSatisfecha solicitud = SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, usuarioId, clave);

        assertThat(solicitud.getStockSistema()).isNull();
        assertThat(solicitud.getClaveIdempotencia()).hasSize(SolicitudNoSatisfecha.LONGITUD_MAXIMA_CLAVE);
    }

    @Test
    void validaDatosObligatoriosYLimites() {
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                null, varianteId, 1, null, ahora, usuarioId, "key"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, null, 1, null, ahora, usuarioId, "key"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, null, "key"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 0, null, ahora, usuarioId, "key"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, -1, null, ahora, usuarioId, "key"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, usuarioId, null))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, usuarioId, "  "))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, usuarioId, "x".repeat(101)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void anulaUnaVezYReconstituyeEstadoPersistido() {
        SolicitudNoSatisfecha solicitud = SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 2, 0, ahora, usuarioId, "req-2");
        UUID anuladaPor = UUID.randomUUID();
        solicitud.anular(anuladaPor, ahora.plusSeconds(30));

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(solicitud.getAnuladaPor()).isEqualTo(anuladaPor);
        assertThat(solicitud.getAnuladaEn()).isEqualTo(ahora.plusSeconds(30));
        assertThatThrownBy(() -> solicitud.anular(anuladaPor, ahora.plusSeconds(60)))
                .isInstanceOf(SolicitudYaAnuladaException.class);

        SolicitudNoSatisfecha reconstituida = SolicitudNoSatisfecha.reconstituir(
                solicitud.getId(), tiendaId, varianteId, 2, 0, ahora, usuarioId, "req-2",
                EstadoSolicitud.ANULADA, anuladaPor, ahora.plusSeconds(30));
        assertThat(reconstituida.getId()).isEqualTo(solicitud.getId());
        assertThat(reconstituida.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(reconstituida.getAnuladaPor()).isEqualTo(anuladaPor);
        assertThat(reconstituida.getAnuladaEn()).isEqualTo(ahora.plusSeconds(30));
    }

    @Test
    void requiereUsuarioParaAnular() {
        SolicitudNoSatisfecha solicitud = SolicitudNoSatisfecha.registrar(
                tiendaId, varianteId, 1, null, ahora, usuarioId, "req-3");

        assertThatThrownBy(() -> solicitud.anular(null, ahora))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
