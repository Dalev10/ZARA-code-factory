package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.OrdenInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrdenReabastecimiento (HU-45)")
class OrdenReabastecimientoTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T10:00:00Z");
    private final UUID tienda = UUID.randomUUID();
    private final UUID variante = UUID.randomUUID();
    private final UUID cd1 = UUID.randomUUID();
    private final UUID cd2 = UUID.randomUUID();

    @Test
    @DisplayName("al iniciar: pendiente = S, despachado 0, sin CD ni intentos")
    void iniciar() {
        var orden = OrdenReabastecimiento.iniciar(null, tienda, variante, 10, AHORA);

        assertThat(orden.getCantidadTotalSolicitada()).isEqualTo(10);
        assertThat(orden.getCantidadPendiente()).isEqualTo(10);
        assertThat(orden.getCantidadDespachadaAcumulada()).isZero();
        assertThat(orden.reservaRestante()).isEqualTo(10);
        assertThat(orden.tieneCd()).isFalse();
        assertThat(orden.getIntentos()).isEmpty();
    }

    @Test
    @DisplayName("descarta el primer CD y asigna el segundo: APROBADA con dos intentos")
    void fallbackAlSegundo() {
        var orden = OrdenReabastecimiento.iniciar(null, tienda, variante, 10, AHORA);

        orden.registrarSinStock(cd1, 1, 4, AHORA);
        orden.asignar(cd2, 2, AHORA);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.APROBADA);
        assertThat(orden.getCdAsignadoId()).isEqualTo(cd2);
        assertThat(orden.getIntentos()).extracting(IntentoCd::resultado)
                .containsExactly(ResultadoIntentoCd.SIN_STOCK, ResultadoIntentoCd.ASIGNADO);
        assertThat(orden.getIntentos().get(0).motivo()).contains("4").contains("10");
        assertThat(orden.getIntentos().get(1).motivo()).isNull();
    }

    @Test
    @DisplayName("ningún CD cubre: GENERADA sin CD y con todos los intentos")
    void sinCdConIntentos() {
        var orden = OrdenReabastecimiento.iniciar(null, tienda, variante, 10, AHORA);

        orden.registrarSinStock(cd1, 1, 0, AHORA);
        orden.registrarSinStock(cd2, 2, 9, AHORA);
        orden.quedarSinCd();

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(orden.getCdAsignadoId()).isNull();
        assertThat(orden.getIntentos()).hasSize(2);
    }

    @Test
    @DisplayName("tienda sin CDs: GENERADA sin CD y sin intentos")
    void tiendaSinCds() {
        var orden = OrdenReabastecimiento.iniciar(null, tienda, variante, 10, AHORA);
        orden.quedarSinCd();

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(orden.getIntentos()).isEmpty();
        assertThat(orden.getMotivoAsignacion()).contains("no tiene CDs");
    }

    @Test
    @DisplayName("terminado el fallback no admite más intentos")
    void fallbackCerrado() {
        var orden = OrdenReabastecimiento.iniciar(null, tienda, variante, 10, AHORA);
        orden.asignar(cd1, 1, AHORA);

        assertThatThrownBy(() -> orden.registrarSinStock(cd2, 2, 0, AHORA))
                .isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    @DisplayName("la cantidad solicitada debe ser mayor que cero")
    void cantidadInvalida() {
        assertThatThrownBy(() -> OrdenReabastecimiento.iniciar(null, tienda, variante, 0, AHORA))
                .isInstanceOf(OrdenInvalidaException.class);
    }
}
