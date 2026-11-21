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

    @Test
    void laSalidaPoneEnCursoYLasParadasEnTransito() {
        Viaje viaje = viajeDeDosParadas();
        Instant salida = ahora.plusSeconds(60);

        viaje.registrarSalida(salida, ahora);

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_CURSO);
        assertThat(viaje.getFechaSalida()).isEqualTo(salida);
        assertThat(viaje.getParadas()).allMatch(p -> p.getEstado() == EstadoParada.EN_TRANSITO
                && salida.equals(p.getFechaSalidaCd()));
        assertThatThrownBy(() -> viaje.registrarSalida(salida, ahora))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.ViajeEstadoInvalidoException.class);
    }

    @Test
    void noSalgoAntesDelDespacho() {
        Viaje viaje = viajeDeDosParadas();

        assertThatThrownBy(() -> viaje.registrarSalida(ahora.minusSeconds(1), ahora))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.CronologiaViajeInvalidaException.class);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.ARMADO);
    }

    @Test
    void conLaUltimaLlegadaElViajeSeCierraConLaMasTardia() {
        Viaje viaje = viajeDeDosParadas();
        viaje.registrarSalida(ahora, ahora);
        ParadaViaje primera = viaje.getParadas().get(0);
        ParadaViaje segunda = viaje.getParadas().get(1);

        viaje.registrarLlegada(segunda.getId(), ahora.plusSeconds(7200));
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_CURSO);
        viaje.registrarLlegada(primera.getId(), ahora.plusSeconds(3600));

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.CERRADO);
        assertThat(viaje.getFechaCierre()).isEqualTo(ahora.plusSeconds(7200));
        assertThat(viaje.getParadas()).allMatch(p -> p.getEstado() == EstadoParada.ENTREGADO);
    }

    @Test
    void lasLlegadasInvalidas() {
        Viaje viaje = viajeDeDosParadas();
        UUID parada = viaje.getParadas().get(0).getId();

        assertThatThrownBy(() -> viaje.registrarLlegada(parada, ahora))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.ViajeEstadoInvalidoException.class);
        viaje.registrarSalida(ahora, ahora);
        assertThatThrownBy(() -> viaje.registrarLlegada(UUID.randomUUID(), ahora))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.ParadaDeViajeNoEncontradaException.class);
        assertThatThrownBy(() -> viaje.registrarLlegada(parada, ahora.minusSeconds(1)))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.CronologiaViajeInvalidaException.class);
        viaje.registrarLlegada(parada, ahora);
        assertThatThrownBy(() -> viaje.registrarLlegada(parada, ahora))
                .isInstanceOf(com.codefactory.supplychain.logistica.domain.exception.ViajeEstadoInvalidoException.class);
    }

    private Viaje viajeDeDosParadas() {
        return Viaje.armar(cdId, "Camión", List.of(new NuevaParada(UUID.randomUUID(), UUID.randomUUID(), null),
                new NuevaParada(UUID.randomUUID(), UUID.randomUUID(), null)), ahora);
    }
}
