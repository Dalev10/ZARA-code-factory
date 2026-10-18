package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifica que {@link ConsultarExistenciasAdapter} traduce correctamente las llamadas
 * entre el puerto de reposicion y la API pública de inventario.
 *
 * Módulo: reposicion — Reposición interna.
 */
@DisplayName("ConsultarExistenciasAdapter")
class ConsultarExistenciasAdapterTest {

    private final ConsultaInventario consultaInventario = mock(ConsultaInventario.class);
    private final ConsultarNodosPort consultarNodos     = mock(ConsultarNodosPort.class);
    private final ConsultarExistenciasAdapter adaptador =
            new ConsultarExistenciasAdapter(consultaInventario, consultarNodos);

    private static final UUID TIENDA_ID   = UUID.randomUUID();
    private static final UUID VARIANTE_ID = UUID.randomUUID();
    private static final UUID NODO_ALMACEN = UUID.randomUUID();
    private static final UUID NODO_BODEGA  = UUID.randomUUID();

    // ---- consultarStockAlmacen -----------------------------------------------

    @Nested
    @DisplayName("consultarStockAlmacen")
    class ConsultarStockAlmacen {

        @Test
        @DisplayName("devuelve aLaMano del almacén cuando el nodo existe y tiene stock")
        void devuelveStockAlmacen() {
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));
            when(consultaInventario.consultarStock(NODO_ALMACEN, VARIANTE_ID))
                    .thenReturn(Optional.of(new StockResumen(NODO_ALMACEN, VARIANTE_ID, 12, 12, 0, 0, 0)));

            assertThat(adaptador.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID)).contains(12);
        }

        @Test
        @DisplayName("devuelve vacío si la tienda no tiene nodo de tipo ALMACEN")
        void devuelveVacioSiNoHayNodoAlmacen() {
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.empty());

            assertThat(adaptador.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID)).isEmpty();
            verifyNoInteractions(consultaInventario);
        }

        @Test
        @DisplayName("devuelve vacío si no hay fila de inventario para la variante en el almacén")
        void devuelveVacioSiNoHayFilaEnInventario() {
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));
            when(consultaInventario.consultarStock(NODO_ALMACEN, VARIANTE_ID)).thenReturn(Optional.empty());

            assertThat(adaptador.consultarStockAlmacen(TIENDA_ID, VARIANTE_ID)).isEmpty();
        }
    }

    // ---- consultarStockBodega ------------------------------------------------

    @Nested
    @DisplayName("consultarStockBodega")
    class ConsultarStockBodega {

        @Test
        @DisplayName("devuelve aLaMano de la bodega cuando el nodo existe y tiene stock")
        void devuelveStockBodega() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.of(NODO_BODEGA));
            when(consultaInventario.consultarStock(NODO_BODEGA, VARIANTE_ID))
                    .thenReturn(Optional.of(new StockResumen(NODO_BODEGA, VARIANTE_ID, 30, 30, 0, 0, 0)));

            assertThat(adaptador.consultarStockBodega(TIENDA_ID, VARIANTE_ID)).contains(30);
        }

        @Test
        @DisplayName("devuelve vacío si la tienda no tiene nodo de tipo BODEGA_TIENDA")
        void devuelveVacioSiNoHayNodoBodega() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.empty());

            assertThat(adaptador.consultarStockBodega(TIENDA_ID, VARIANTE_ID)).isEmpty();
            verifyNoInteractions(consultaInventario);
        }

        @Test
        @DisplayName("devuelve vacío si no hay fila de inventario para la variante en la bodega")
        void devuelveVacioSiNoHayFilaEnInventario() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.of(NODO_BODEGA));
            when(consultaInventario.consultarStock(NODO_BODEGA, VARIANTE_ID)).thenReturn(Optional.empty());

            assertThat(adaptador.consultarStockBodega(TIENDA_ID, VARIANTE_ID)).isEmpty();
        }
    }

    // ---- variantesConInventarioEnAlmacen ------------------------------------

    @Nested
    @DisplayName("variantesConInventarioEnAlmacen")
    class VariantesConInventario {

        @Test
        @DisplayName("devuelve los ids de todas las variantes con stock en el almacén")
        void devuelveListaDeVariantes() {
            UUID variante1 = UUID.randomUUID();
            UUID variante2 = UUID.randomUUID();
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));
            when(consultaInventario.consultarExistenciasPorNodo(NODO_ALMACEN)).thenReturn(List.of(
                    new StockResumen(NODO_ALMACEN, variante1, 5, 5, 0, 0, 0),
                    new StockResumen(NODO_ALMACEN, variante2, 2, 2, 0, 0, 0)
            ));

            List<UUID> resultado = adaptador.variantesConInventarioEnAlmacen(TIENDA_ID);

            assertThat(resultado).containsExactlyInAnyOrder(variante1, variante2);
        }

        @Test
        @DisplayName("devuelve lista vacía si la tienda no tiene nodo de almacén")
        void devuelveVacioSiNoHayNodoAlmacen() {
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.empty());

            assertThat(adaptador.variantesConInventarioEnAlmacen(TIENDA_ID)).isEmpty();
            verifyNoInteractions(consultaInventario);
        }

        @Test
        @DisplayName("devuelve lista vacía si el nodo no tiene ninguna fila de inventario")
        void devuelveVacioSiAlmacenSinInventario() {
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));
            when(consultaInventario.consultarExistenciasPorNodo(NODO_ALMACEN)).thenReturn(List.of());

            assertThat(adaptador.variantesConInventarioEnAlmacen(TIENDA_ID)).isEmpty();
        }
    }
}