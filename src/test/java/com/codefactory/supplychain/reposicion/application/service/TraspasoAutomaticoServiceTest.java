package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort.UmbralAplicable;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("TraspasoAutomaticoService")
@ExtendWith(MockitoExtension.class)
class TraspasoAutomaticoServiceTest {

    @Mock ResolverUmbralPort resolverUmbral;
    @Mock ConsultarExistenciasPort consultarExistencias;
    @Mock EjecutarTraspasoPort ejecutarTraspaso;
    @Mock TraspasoInternoRepository traspasoRepository;

    @InjectMocks
    TraspasoAutomaticoService servicio;

    private static final UUID TIENDA_ID   = UUID.randomUUID();
    private static final UUID VARIANTE_ID = UUID.randomUUID();

    @BeforeEach
    void stubRepository() {
        // Por defecto, guardar devuelve lo que recibe
        lenient().when(traspasoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("cuando no hay umbral configurado")
    class SinUmbral {

        @Test
        @DisplayName("devuelve vacío y no ejecuta traspaso")
        void sinUmbralDevuelveVacio() {
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID)).thenReturn(Optional.empty());

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
        }
    }

    @Nested
    @DisplayName("cuando el Almacén está por encima del mínimo")
    class AlmacenSobreMinimo {

        @Test
        @DisplayName("devuelve vacío y no ejecuta traspaso")
        void noDisparaFueraDeUmbral() {
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(5)); // ≥ mínimo

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
        }
    }

    @Nested
    @DisplayName("cuando el Almacén está por debajo del mínimo")
    class AlmacenBajoMinimo {

        @Test
        @DisplayName("ejecuta el traspaso con la cantidad correcta (D-54)")
        void ejecutaTraspasoConCantidadCorrecta() {
            // almacén=3, bodega=20, objetivo=10 → cantidad=7
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(3));
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(20));

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isPresent();
            TraspasoInterno traspaso = resultado.get();
            assertThat(traspaso.getCantidad()).isEqualTo(7);
            assertThat(traspaso.getOrigen()).isEqualTo(OrigenTraspaso.AUTOMATICO);
            assertThat(traspaso.getRegistradoPor()).isNull();

            verify(ejecutarTraspaso).traspasar(TIENDA_ID, VARIANTE_ID, 7, traspaso.getId());
            verify(traspasoRepository).guardar(any());
        }

        @Test
        @DisplayName("traspaso parcial cuando la bodega no alcanza para cubrir el objetivo")
        void traspasoParcialBodegaInsuficiente() {
            // almacén=0, bodega=3, objetivo=10 → cantidad=3
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(0));
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(3));

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCantidad()).isEqualTo(3);
        }

        @Test
        @DisplayName("devuelve vacío si la bodega está vacía aunque haya déficit")
        void bodegaVaciaNoGeneraTraspaso() {
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(2));
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(0));

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
        }

        @Test
        @DisplayName("almacén negativo (venta forzada) usa MAX(almacén,0)=0 para calcular (D-54)")
        void almacenNegativoUsaMaxConCero() {
            // almacén=-5 → MAX=0, déficit=10; bodega=8 → cantidad=8
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(-5));
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(8));

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCantidad()).isEqualTo(8);
        }

        @Test
        @DisplayName("sin fila en inventario se trata como stock=0 y no ejecuta si bodega también es 0")
        void sinFilaEnInventarioTrataComoStock0() {
            when(resolverUmbral.resolver(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.of(new UmbralAplicable(5, 10)));
            when(consultarExistencias.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.empty()); // sin fila → 0
            when(consultarExistencias.consultarStockBodega(TIENDA_ID, VARIANTE_ID))
                    .thenReturn(Optional.empty()); // sin fila → 0

            Optional<TraspasoInterno> resultado = servicio.evaluar(TIENDA_ID, VARIANTE_ID);

            // almacén=0 < mínimo=5, pero bodega=0 → no hay nada que traspasar
            assertThat(resultado).isEmpty();
        }
    }
}
