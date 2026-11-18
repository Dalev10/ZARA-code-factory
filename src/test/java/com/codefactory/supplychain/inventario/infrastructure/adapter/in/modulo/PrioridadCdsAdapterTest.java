package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.application.port.out.PrioridadTiendaCdRepository;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrioridadCdsAdapterTest {

    private final PrioridadTiendaCdRepository prioridades = mock(PrioridadTiendaCdRepository.class);
    private final PrioridadCdsAdapter adapter = new PrioridadCdsAdapter(prioridades);

    private final UUID tienda = UUID.randomUUID();

    @Test
    void devuelveLosCdsEnElOrdenDePrioridad() {
        UUID cdPrimero = UUID.randomUUID();
        UUID cdSegundo = UUID.randomUUID();
        when(prioridades.listarPorTienda(tienda)).thenReturn(List.of(
                PrioridadTiendaCd.crear(tienda, cdPrimero, 1),
                PrioridadTiendaCd.crear(tienda, cdSegundo, 2)));

        assertThat(adapter.cdsPorPrioridad(tienda)).containsExactly(cdPrimero, cdSegundo);
    }

    @Test
    void tiendaSinCdsDevuelveListaVacia() {
        when(prioridades.listarPorTienda(tienda)).thenReturn(List.of());

        assertThat(adapter.cdsPorPrioridad(tienda)).isEmpty();
    }

    @Test
    void rechazaTiendaNula() {
        assertThatThrownBy(() -> adapter.cdsPorPrioridad(null))
                .isInstanceOf(NullPointerException.class);
    }
}
