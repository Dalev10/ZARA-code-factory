package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.RecomendacionInvalidaException;
import com.codefactory.supplychain.reposicion.domain.exception.RecomendacionYaResueltaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Recomendacion: aprobar y rechazar (HU-44)")
class RecomendacionResolucionTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-21T14:05:00Z");

    private static Recomendacion borrador() {
        return Recomendacion.evaluar(UUID.randomUUID(), UUID.randomUUID(), 3, 5, 15, null,
                Instant.parse("2026-11-20T10:00:00Z")).orElseThrow();
    }

    @Test
    @DisplayName("aprobar sin cantidad aprueba la sugerida, sin desviación")
    void aprobarSinCantidad() {
        Recomendacion aprobada = borrador().aprobar(null, USUARIO, AHORA);

        assertThat(aprobada.getEstado()).isEqualTo(EstadoRecomendacion.APROBADA);
        assertThat(aprobada.getCantidadAprobada()).isEqualTo(12);
        assertThat(aprobada.desviacion()).contains(0);
        assertThat(aprobada.getAprobadoPor()).isEqualTo(USUARIO);
        assertThat(aprobada.getFechaAprobacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("aprobar con cantidad editada guarda la cantidad y calcula la desviación")
    void aprobarConCantidadEditada() {
        Recomendacion aprobada = borrador().aprobar(15, USUARIO, AHORA);

        assertThat(aprobada.getCantidadAprobada()).isEqualTo(15);
        assertThat(aprobada.getCantidadSugerida()).isEqualTo(12);
        assertThat(aprobada.desviacion()).contains(3);
    }

    @Test
    @DisplayName("la desviación puede ser negativa")
    void desviacionNegativa() {
        assertThat(borrador().aprobar(8, USUARIO, AHORA).desviacion()).contains(-4);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("aprobar con cantidad menor o igual a cero falla y no cambia el original")
    void cantidadInvalida(int cantidad) {
        Recomendacion original = borrador();

        assertThatThrownBy(() -> original.aprobar(cantidad, USUARIO, AHORA))
                .isInstanceOf(RecomendacionInvalidaException.class);
        assertThat(original.getEstado()).isEqualTo(EstadoRecomendacion.BORRADOR);
    }

    @Test
    @DisplayName("rechazar deja RECHAZADA sin datos de aprobación")
    void rechazar() {
        Recomendacion rechazada = borrador().rechazar();

        assertThat(rechazada.getEstado()).isEqualTo(EstadoRecomendacion.RECHAZADA);
        assertThat(rechazada.getCantidadAprobada()).isNull();
        assertThat(rechazada.desviacion()).isEmpty();
    }

    @Test
    @DisplayName("una recomendación ya resuelta no se puede aprobar ni rechazar")
    void yaResuelta() {
        Recomendacion aprobada = borrador().aprobar(null, USUARIO, AHORA);
        Recomendacion rechazada = borrador().rechazar();

        assertThatThrownBy(() -> aprobada.aprobar(null, USUARIO, AHORA))
                .isInstanceOf(RecomendacionYaResueltaException.class);
        assertThatThrownBy(aprobada::rechazar).isInstanceOf(RecomendacionYaResueltaException.class);
        assertThatThrownBy(() -> rechazada.aprobar(null, USUARIO, AHORA))
                .isInstanceOf(RecomendacionYaResueltaException.class);
        assertThatThrownBy(rechazada::rechazar).isInstanceOf(RecomendacionYaResueltaException.class);
    }

    @Test
    @DisplayName("aprobar conserva la versión para el bloqueo optimista")
    void conservaVersion() {
        Recomendacion persistida = Recomendacion.reconstruir(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, 5, EstadoRecomendacion.BORRADOR, AHORA, null, null, null, 7L);

        assertThat(persistida.aprobar(null, USUARIO, AHORA).getVersion()).isEqualTo(7L);
        assertThat(persistida.rechazar().getVersion()).isEqualTo(7L);
    }
}
