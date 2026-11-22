package com.codefactory.supplychain.inventario.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultadoReservaTest {

    @Test
    void asignadoIndicaReservaAplicada() {
        ResultadoReserva resultado = ResultadoReserva.asignado(4);

        assertThat(resultado.estado()).isEqualTo(EstadoReserva.ASIGNADO);
        assertThat(resultado.asignada()).isTrue();
        assertThat(resultado.disponibleActual()).isEqualTo(4);
    }

    @Test
    void sinDisponibleConservaElDisponibleQueImpidioReservar() {
        ResultadoReserva resultado = ResultadoReserva.sinDisponible(3);

        assertThat(resultado.estado()).isEqualTo(EstadoReserva.SIN_DISPONIBLE);
        assertThat(resultado.asignada()).isFalse();
        assertThat(resultado.disponibleActual()).isEqualTo(3);
    }

    @Test
    void rechazaEstadoNulo() {
        assertThatThrownBy(() -> new ResultadoReserva(null, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
