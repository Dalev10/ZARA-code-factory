package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;

import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Verifica que {@link EjecutarTraspasoAdapter} resuelve los nodos de la tienda y llama
 * a {@link InventarioMovimientos#trasladar} con los parámetros correctos.
 *
 * Módulo: reposicion — Reposición interna.
 */
@DisplayName("EjecutarTraspasoAdapter")
class EjecutarTraspasoAdapterTest {

    private final InventarioMovimientos inventarioMovimientos = mock(InventarioMovimientos.class);
    private final ConsultarNodosPort consultarNodos           = mock(ConsultarNodosPort.class);
    private final EjecutarTraspasoAdapter adaptador =
            new EjecutarTraspasoAdapter(inventarioMovimientos, consultarNodos);

    private static final UUID TIENDA_ID    = UUID.randomUUID();
    private static final UUID VARIANTE_ID  = UUID.randomUUID();
    private static final UUID TRASPASO_ID  = UUID.randomUUID();
    private static final UUID NODO_BODEGA  = UUID.randomUUID();
    private static final UUID NODO_ALMACEN = UUID.randomUUID();

    @Nested
    @DisplayName("traspaso exitoso")
    class TraspasoExitoso {

        @Test
        @DisplayName("llama a trasladar con bodega como origen y almacén como destino")
        void llamaTrasladerConNodosCorrectos() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.of(NODO_BODEGA));
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));

            assertThat(adaptador.traspasar(TIENDA_ID, VARIANTE_ID, 7, TRASPASO_ID)).isTrue();

            verify(inventarioMovimientos).trasladar(NODO_BODEGA, NODO_ALMACEN, VARIANTE_ID, 7, TRASPASO_ID);
        }

        @Test
        @DisplayName("devuelve false si inventario rechaza el traspaso por stock insuficiente")
        void devuelveFalseSinStock() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.of(NODO_BODEGA));
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.of(NODO_ALMACEN));
            org.mockito.Mockito.doThrow(new StockInsuficienteException())
                    .when(inventarioMovimientos).trasladar(NODO_BODEGA, NODO_ALMACEN, VARIANTE_ID, 7, TRASPASO_ID);

            assertThat(adaptador.traspasar(TIENDA_ID, VARIANTE_ID, 7, TRASPASO_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("nodo no encontrado")
    class NodoNoEncontrado {

        @Test
        @DisplayName("responde 404 si la tienda no tiene nodo de BODEGA_TIENDA")
        void lanzaSiBodegaNoExiste() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adaptador.traspasar(TIENDA_ID, VARIANTE_ID, 5, TRASPASO_ID))
                    .isInstanceOf(ReposicionRecursoNoEncontradoException.class)
                    .hasMessageContaining(TIENDA_ID.toString());

            verifyNoInteractions(inventarioMovimientos);
        }

        @Test
        @DisplayName("responde 404 si la tienda no tiene nodo de ALMACEN")
        void lanzaSiAlmacenNoExiste() {
            when(consultarNodos.buscarNodoBodega(TIENDA_ID)).thenReturn(Optional.of(NODO_BODEGA));
            when(consultarNodos.buscarNodoAlmacen(TIENDA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adaptador.traspasar(TIENDA_ID, VARIANTE_ID, 5, TRASPASO_ID))
                    .isInstanceOf(ReposicionRecursoNoEncontradoException.class)
                    .hasMessageContaining(TIENDA_ID.toString());

            verifyNoInteractions(inventarioMovimientos);
        }
    }
}