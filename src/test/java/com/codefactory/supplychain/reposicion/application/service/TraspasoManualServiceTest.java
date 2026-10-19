package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.domain.exception.BodegaInsuficienteException;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("TraspasoManualService")
@ExtendWith(MockitoExtension.class)
class TraspasoManualServiceTest {

    @Mock ConsultarExistenciasPort consultarExistencias;
    @Mock EjecutarTraspasoPort ejecutarTraspaso;
    @Mock TraspasoInternoRepository traspasoRepository;

    @InjectMocks
    TraspasoManualService servicio;

    private static final UUID TIENDA_ID      = UUID.randomUUID();
    private static final UUID VARIANTE_ID    = UUID.randomUUID();
    private static final UUID REGISTRADO_POR = UUID.randomUUID();

    @BeforeEach
    void stubRepository() {
        lenient().when(traspasoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("registro exitoso")
    class RegistroExitoso {

        @Test
        @DisplayName("persiste traspaso MANUAL con el registrador correcto cuando hay stock suficiente")
        void persiste_con_registrador_y_origen_manual() {
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(20));

            TraspasoInterno resultado = servicio.registrar(TIENDA_ID, VARIANTE_ID, 7, REGISTRADO_POR);

            assertThat(resultado.getCantidad()).isEqualTo(7);
            assertThat(resultado.getOrigen()).isEqualTo(OrigenTraspaso.MANUAL);
            assertThat(resultado.getRegistradoPor()).isEqualTo(REGISTRADO_POR);
            assertThat(resultado.getTiendaId()).isEqualTo(TIENDA_ID);
            assertThat(resultado.getVarianteId()).isEqualTo(VARIANTE_ID);

            verify(ejecutarTraspaso).traspasar(TIENDA_ID, VARIANTE_ID, 7, resultado.getId());
            verify(traspasoRepository).guardar(any());
        }

        @Test
        @DisplayName("traspaso exactamente igual al stock disponible en Bodega es válido (límite)")
        void traspaso_exactamente_el_stock_disponible() {
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(5));

            TraspasoInterno resultado = servicio.registrar(TIENDA_ID, VARIANTE_ID, 5, REGISTRADO_POR);

            assertThat(resultado.getCantidad()).isEqualTo(5);
            verify(ejecutarTraspaso).traspasar(TIENDA_ID, VARIANTE_ID, 5, resultado.getId());
        }
    }

    @Nested
    @DisplayName("stock insuficiente")
    class StockInsuficiente {

        @Test
        @DisplayName("lanza BodegaInsuficienteException si la cantidad supera el stock de la Bodega (D-56)")
        void lanza_cuando_bodega_insuficiente() {
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(3));

            assertThatThrownBy(() ->
                    servicio.registrar(TIENDA_ID, VARIANTE_ID, 10, REGISTRADO_POR))
                    .isInstanceOf(BodegaInsuficienteException.class)
                    .hasMessageContaining("disponible=3")
                    .hasMessageContaining("solicitado=10");

            verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
        }

        @Test
        @DisplayName("lanza BodegaInsuficienteException si la Bodega no tiene fila (stock=0)")
        void lanza_cuando_bodega_sin_fila() {
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.empty()); // sin fila → 0

            assertThatThrownBy(() ->
                    servicio.registrar(TIENDA_ID, VARIANTE_ID, 1, REGISTRADO_POR))
                    .isInstanceOf(BodegaInsuficienteException.class);

            verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
        }
    }
}
