package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.CdSinStockParaDespacharException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CicloDespachoTest {

    private final UUID ordenId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-11-21T15:00:00Z");

    @Test
    void conStockSuficienteDespachaTodoYQuedaCompleto() {
        CicloDespacho ciclo = CicloDespacho.despachar(ordenId, 1, 10, 30, ahora);

        assertThat(ciclo.getCantidadDespachada()).isEqualTo(10);
        assertThat(ciclo.getCantidadPendiente()).isZero();
        assertThat(ciclo.getResultado()).isEqualTo(ResultadoDespacho.COMPLETO);
        assertThat(ciclo.getFillRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(ciclo.getFechaDespacho()).isEqualTo(ahora);
    }

    @Test
    void conMenosALaManoDespachaLoQueHayYQuedaParcial() {
        CicloDespacho ciclo = CicloDespacho.despachar(ordenId, 2, 3, 2, ahora);

        assertThat(ciclo.getCantidadDespachada()).isEqualTo(2);
        assertThat(ciclo.getCantidadPendiente()).isEqualTo(1);
        assertThat(ciclo.getResultado()).isEqualTo(ResultadoDespacho.PARCIAL);
        assertThat(ciclo.getFillRate()).isEqualByComparingTo("0.6667");
        assertThat(ciclo.getNumeroCiclo()).isEqualTo(2);
    }

    @Test
    void sinNadaALaManoNoHayCiclo() {
        assertThatThrownBy(() -> CicloDespacho.despachar(ordenId, 1, 10, 0, ahora))
                .isInstanceOf(CdSinStockParaDespacharException.class);
        assertThatThrownBy(() -> CicloDespacho.despachar(ordenId, 1, 10, -3, ahora))
                .isInstanceOf(CdSinStockParaDespacharException.class);
    }

    @Test
    void unaOrdenSinPendienteNoSeDespacha() {
        assertThatThrownBy(() -> CicloDespacho.despachar(ordenId, 1, 0, 10, ahora))
                .isInstanceOf(IllegalStateException.class);
    }
}
