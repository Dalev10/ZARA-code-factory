package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.inventario.application.port.out.ExistenciasRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultaInventarioAdapterTest {

    private final ExistenciasRepositoryPort existencias = mock(ExistenciasRepositoryPort.class);
    private final ConsultaInventarioAdapter adapter = new ConsultaInventarioAdapter(existencias);

    private final UUID nodo = UUID.randomUUID();

    @Test
    void consultarStockDevuelveLasMagnitudesCuandoHayCarga() {
        UUID variante = UUID.randomUUID();
        StockResumen stock = new StockResumen(nodo, variante, 10, 8, 2, 1, 5);
        when(existencias.buscarPorNodoYVariante(nodo, variante)).thenReturn(Optional.of(stock));

        assertThat(adapter.consultarStock(nodo, variante)).contains(stock);
    }

    @Test
    void consultarStockSinCargaDevuelveVacio() {
        UUID variante = UUID.randomUUID();
        when(existencias.buscarPorNodoYVariante(nodo, variante)).thenReturn(Optional.empty());

        assertThat(adapter.consultarStock(nodo, variante)).isEmpty();
    }

    @Test
    void consultarStockRechazaParametrosNulos() {
        assertThatThrownBy(() -> adapter.consultarStock(null, UUID.randomUUID()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.consultarStock(nodo, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void existenciasPorNodoIncluyeSaldoCeroYOrdenaPorVariante() {
        UUID v1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID v2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        StockResumen conStock = new StockResumen(nodo, v2, 5, 5, 0, 0, 0);
        StockResumen sinStock = new StockResumen(nodo, v1, 0, 0, 0, 0, 0);
        when(existencias.listarPorNodo(nodo)).thenReturn(List.of(conStock, sinStock));

        assertThat(adapter.consultarExistenciasPorNodo(nodo)).containsExactly(sinStock, conStock);
    }

    @Test
    void existenciasPorNodoSinCargasDevuelveListaVacia() {
        when(existencias.listarPorNodo(nodo)).thenReturn(List.of());

        assertThat(adapter.consultarExistenciasPorNodo(nodo)).isEmpty();
    }
}
