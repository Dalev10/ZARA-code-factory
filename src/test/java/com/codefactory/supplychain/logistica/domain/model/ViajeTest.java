package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.ViajeInvalidoException;
import com.codefactory.supplychain.logistica.domain.model.Viaje.NuevaParada;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViajeTest {

    private final UUID cdId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-11-21T15:00:00Z");

    @Test
    void armaElViajeConLasParadasPendientesYEnOrden() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();

        Viaje viaje = Viaje.armar(cdId, " Camión 1 ", List.of(new NuevaParada(c1, tienda, 2),
                new NuevaParada(c2, tienda, 1)), ahora);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.ARMADO);
        assertThat(viaje.getVehiculo()).isEqualTo("Camión 1");
        assertThat(viaje.getParadas()).extracting(ParadaViaje::getCicloDespachoId).containsExactly(c2, c1);
        assertThat(viaje.getParadas()).allMatch(p -> p.getEstado() == EstadoParada.PENDIENTE);
    }

    @Test
    void sinOrdenUsaLaPosicionEnLaLista() {
        Viaje viaje = Viaje.armar(cdId, "Camión", List.of(new NuevaParada(UUID.randomUUID(), UUID.randomUUID(), null),
                new NuevaParada(UUID.randomUUID(), UUID.randomUUID(), null)), ahora);

        assertThat(viaje.getParadas()).extracting(ParadaViaje::getOrdenParada).containsExactly(1, 2);
    }

    @Test
    void rechazaLasFormasInvalidas() {
        UUID ciclo = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();

        assertThatThrownBy(() -> Viaje.armar(cdId, "Camión", List.of(), ahora))
                .isInstanceOf(ViajeInvalidoException.class);
        assertThatThrownBy(() -> Viaje.armar(cdId, " ", List.of(new NuevaParada(ciclo, tienda, null)), ahora))
                .isInstanceOf(ViajeInvalidoException.class);
        assertThatThrownBy(() -> Viaje.armar(cdId, "Camión",
                List.of(new NuevaParada(ciclo, tienda, null), new NuevaParada(ciclo, tienda, null)), ahora))
                .isInstanceOf(ViajeInvalidoException.class).hasMessageContaining("repetido");
        assertThatThrownBy(() -> Viaje.armar(cdId, "Camión", List.of(new NuevaParada(ciclo, tienda, 1),
                new NuevaParada(UUID.randomUUID(), tienda, 1)), ahora))
                .isInstanceOf(ViajeInvalidoException.class).hasMessageContaining("orden de parada");
        assertThatThrownBy(() -> Viaje.armar(cdId, "Camión", List.of(new NuevaParada(ciclo, tienda, 0)), ahora))
                .isInstanceOf(ViajeInvalidoException.class);
    }
}
