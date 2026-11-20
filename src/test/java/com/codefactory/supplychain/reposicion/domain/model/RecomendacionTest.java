package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.RecomendacionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Recomendacion (HU-43)")
class RecomendacionTest {

    private static final UUID TIENDA = UUID.randomUUID();
    private static final UUID VARIANTE = UUID.randomUUID();
    private static final UUID CD = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-17T10:00:00Z");

    @Test
    @DisplayName("pronosticado bajo el mínimo: BORRADOR con objetivo − pronosticado")
    void bajoElMinimoCreaBorrador() {
        var recomendacion = Recomendacion.evaluar(TIENDA, VARIANTE, 3, 5, 15, CD, AHORA).orElseThrow();

        assertThat(recomendacion.getEstado()).isEqualTo(EstadoRecomendacion.BORRADOR);
        assertThat(recomendacion.getCantidadSugerida()).isEqualTo(12);
        assertThat(recomendacion.getCdSugeridoId()).isEqualTo(CD);
        assertThat(recomendacion.getFechaCreacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("pronosticado igual al mínimo: no recomienda (la regla es estrictamente menor)")
    void enElMinimoNoRecomienda() {
        assertThat(Recomendacion.evaluar(TIENDA, VARIANTE, 5, 5, 15, CD, AHORA)).isEmpty();
    }

    @Test
    @DisplayName("pronosticado negativo: la cantidad sugerida lo compensa")
    void pronosticadoNegativo() {
        var recomendacion = Recomendacion.evaluar(TIENDA, VARIANTE, -2, 5, 15, CD, AHORA).orElseThrow();
        assertThat(recomendacion.getCantidadSugerida()).isEqualTo(17);
    }

    @Test
    @DisplayName("cantidad sugerida no positiva: no recomienda")
    void cantidadNoPositivaNoRecomienda() {
        // Nivel inconsistente (objetivo ≤ pronosticado < mínimo): no se crea nada.
        assertThat(Recomendacion.evaluar(TIENDA, VARIANTE, 4, 5, 4, CD, AHORA)).isEmpty();
    }

    @Test
    @DisplayName("sin CDs en la tienda: el CD sugerido queda nulo")
    void sinCdSugerido() {
        var recomendacion = Recomendacion.evaluar(TIENDA, VARIANTE, 0, 5, 15, null, AHORA).orElseThrow();
        assertThat(recomendacion.getCdSugeridoId()).isNull();
    }

    @Test
    @DisplayName("reconstruir con cantidad no positiva es inválido")
    void reconstruirInvalido() {
        assertThatThrownBy(() -> Recomendacion.reconstruir(UUID.randomUUID(), TIENDA, VARIANTE, CD, 0,
                EstadoRecomendacion.BORRADOR, AHORA))
                .isInstanceOf(RecomendacionInvalidaException.class);
    }
}
