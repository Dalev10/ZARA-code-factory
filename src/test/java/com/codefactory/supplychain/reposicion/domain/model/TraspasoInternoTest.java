package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.TraspasoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TraspasoInterno — dominio puro")
class TraspasoInternoTest {

    private static final UUID VARIANTE_ID = UUID.randomUUID();
    private static final UUID TIENDA_ID   = UUID.randomUUID();
    private static final UUID USUARIO_ID  = UUID.randomUUID();

    @Nested
    @DisplayName("calcularCantidad (D-54)")
    class CalcularCantidad {

        @ParameterizedTest(name = "almacén={0}, bodega={1}, objetivo={2} → {3}")
        @CsvSource({
                // caso base: déficit < bodega → toma el déficit
                "3, 20, 10, 7",
                // bodega limita: bodega < déficit → toma la bodega
                "0, 4, 10, 4",
                // almacén en negativo: MAX(almacén,0)=0, déficit = objetivo
                "-5, 20, 10, 10",
                // almacén en negativo y bodega limita
                "-5, 3, 10, 3",
                // almacén ya en el objetivo → déficit = 0
                "10, 20, 10, 0",
                // almacén por encima del objetivo → negativo (no hay traspaso)
                "15, 20, 10, -5",
                // bodega vacía → 0
                "3, 0, 10, 0",
        })
        void calculaCorrectamente(int almacen, int bodega, int objetivo, int esperado) {
            assertThat(TraspasoInterno.calcularCantidad(almacen, bodega, objetivo))
                    .isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("calcularAutomatico")
    class CalcularAutomatico {

        @Test
        @DisplayName("devuelve traspaso con la cantidad calculada cuando hay déficit y stock en bodega")
        void creaTraspasoCuandoHayDeficit() {
            TraspasoInterno t = TraspasoInterno.calcularAutomatico(
                    VARIANTE_ID, TIENDA_ID, 3, 20, 10);

            assertThat(t).isNotNull();
            assertThat(t.getCantidad()).isEqualTo(7);
            assertThat(t.getOrigen()).isEqualTo(OrigenTraspaso.AUTOMATICO);
            assertThat(t.getRegistradoPor()).isNull();
            assertThat(t.getTimestampTraspaso()).isNotNull();
        }

        @Test
        @DisplayName("devuelve null cuando el Almacén ya está en o sobre el objetivo")
        void devuelveNullSinDeficit() {
            TraspasoInterno t = TraspasoInterno.calcularAutomatico(
                    VARIANTE_ID, TIENDA_ID, 10, 20, 10);
            assertThat(t).isNull();
        }

        @Test
        @DisplayName("devuelve null cuando la Bodega está vacía aunque haya déficit")
        void devuelveNullBodegaVacia() {
            TraspasoInterno t = TraspasoInterno.calcularAutomatico(
                    VARIANTE_ID, TIENDA_ID, 2, 0, 10);
            assertThat(t).isNull();
        }

        @Test
        @DisplayName("almacén negativo (venta forzada) se trata como 0 para el cálculo (D-54)")
        void almacenNegativoTratadoComoZero() {
            // almacén = -5 → MAX(-5,0) = 0 → déficit = 10 − 0 = 10; bodega = 6 → cantidad = 6
            TraspasoInterno t = TraspasoInterno.calcularAutomatico(
                    VARIANTE_ID, TIENDA_ID, -5, 6, 10);

            assertThat(t).isNotNull();
            assertThat(t.getCantidad()).isEqualTo(6);
        }

        @Test
        @DisplayName("traspaso parcial cuando la bodega no alcanza para llegar al objetivo")
        void traspasoParcialCuandoBodegaNoAlcanza() {
            // almacén = 0, bodega = 3, objetivo = 10 → cantidad = MIN(10, 3) = 3
            TraspasoInterno t = TraspasoInterno.calcularAutomatico(
                    VARIANTE_ID, TIENDA_ID, 0, 3, 10);

            assertThat(t).isNotNull();
            assertThat(t.getCantidad()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("reconstruir — invariantes")
    class Reconstruir {

        @Test
        @DisplayName("cantidad = 0 lanza TraspasoInvalidoException")
        void cantidadCeroEsInvalida() {
            assertThatThrownBy(() ->
                    TraspasoInterno.reconstruir(UUID.randomUUID(), VARIANTE_ID, TIENDA_ID,
                            0, OrigenTraspaso.AUTOMATICO, null, java.time.Instant.now()))
                    .isInstanceOf(TraspasoInvalidoException.class)
                    .hasMessageContaining("mayor que cero");
        }

        @Test
        @DisplayName("MANUAL sin registrador lanza TraspasoInvalidoException (D-56)")
        void manualSinRegistradorEsInvalido() {
            assertThatThrownBy(() ->
                    TraspasoInterno.reconstruir(UUID.randomUUID(), VARIANTE_ID, TIENDA_ID,
                            5, OrigenTraspaso.MANUAL, null, java.time.Instant.now()))
                    .isInstanceOf(TraspasoInvalidoException.class)
                    .hasMessageContaining("manual requiere");
        }

        @Test
        @DisplayName("AUTOMATICO con registrador lanza TraspasoInvalidoException (D-56)")
        void automaticoConRegistradorEsInvalido() {
            assertThatThrownBy(() ->
                    TraspasoInterno.reconstruir(UUID.randomUUID(), VARIANTE_ID, TIENDA_ID,
                            5, OrigenTraspaso.AUTOMATICO, USUARIO_ID, java.time.Instant.now()))
                    .isInstanceOf(TraspasoInvalidoException.class)
                    .hasMessageContaining("automático no debe");
        }

        @Test
        @DisplayName("MANUAL con registrador es válido")
        void manualConRegistradorEsValido() {
            TraspasoInterno t = TraspasoInterno.reconstruir(UUID.randomUUID(), VARIANTE_ID,
                    TIENDA_ID, 5, OrigenTraspaso.MANUAL, USUARIO_ID, java.time.Instant.now());

            assertThat(t.getOrigen()).isEqualTo(OrigenTraspaso.MANUAL);
            assertThat(t.getRegistradoPor()).isEqualTo(USUARIO_ID);
        }
    }
}
