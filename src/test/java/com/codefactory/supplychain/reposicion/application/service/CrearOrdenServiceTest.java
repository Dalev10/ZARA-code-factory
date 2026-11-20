package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.CdsPorPrioridadPort;
import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort.Reserva;
import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CrearOrdenService (HU-45)")
class CrearOrdenServiceTest {

    private final CdsPorPrioridadPort cds = mock(CdsPorPrioridadPort.class);
    private final ReservarStockCdPort reservar = mock(ReservarStockCdPort.class);
    private final OrdenReabastecimientoRepository ordenes = mock(OrdenReabastecimientoRepository.class);
    private CrearOrdenService service;

    private final UUID tienda = UUID.randomUUID();
    private final UUID variante = UUID.randomUUID();
    private final UUID cd1 = UUID.randomUUID();
    private final UUID cd2 = UUID.randomUUID();
    private final UUID cd3 = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<Clock> reloj = mock(ObjectProvider.class);
        when(reloj.getIfAvailable(any())).thenReturn(Clock.systemUTC());
        service = new CrearOrdenService(cds, reservar, ordenes, reloj);
        when(ordenes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("se detiene en el primer CD que asigna: no consulta los siguientes")
    void seDetieneEnElPrimeroQueAsigna() {
        when(cds.cdsPorPrioridad(tienda)).thenReturn(List.of(cd1, cd2, cd3));
        when(reservar.reservar(eq(cd1), any(), any(), anyInt(), any())).thenReturn(new Reserva(false, 3));
        when(reservar.reservar(eq(cd2), any(), any(), anyInt(), any())).thenReturn(new Reserva(true, 0));

        var orden = service.crear(null, tienda, variante, 10);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.APROBADA);
        assertThat(orden.getCdAsignadoId()).isEqualTo(cd2);
        assertThat(orden.getIntentos()).hasSize(2);
        verify(reservar, never()).reservar(eq(cd3), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("la reserva pide siempre la cantidad completa (D-83)")
    void pideLaCantidadCompleta() {
        when(cds.cdsPorPrioridad(tienda)).thenReturn(List.of(cd1, cd2));
        when(reservar.reservar(any(), any(), any(), anyInt(), any())).thenReturn(new Reserva(false, 9));

        var orden = service.crear(null, tienda, variante, 10);

        verify(reservar).reservar(eq(cd1), eq(tienda), eq(variante), eq(10), eq(orden.getId()));
        verify(reservar).reservar(eq(cd2), eq(tienda), eq(variante), eq(10), eq(orden.getId()));
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(orden.getIntentos()).extracting(i -> i.ordenEvaluado()).containsExactly(1, 2);
    }
}
