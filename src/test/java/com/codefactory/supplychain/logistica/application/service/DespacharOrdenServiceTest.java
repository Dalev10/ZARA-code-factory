package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.application.port.out.InventarioDespachoPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenDespachoPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenDespachoPort.OrdenADespachar;
import com.codefactory.supplychain.logistica.domain.exception.CdSinStockParaDespacharException;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DespacharOrdenServiceTest {

    private final UUID ordenId = UUID.randomUUID();
    private final UUID cdId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-11-21T15:00:00Z");

    private OrdenDespachoPort ordenes;
    private InventarioDespachoPort inventario;
    private CicloDespachoRepository ciclos;
    private DespacharOrdenService servicio;

    @BeforeEach
    void setUp() {
        ordenes = mock(OrdenDespachoPort.class);
        inventario = mock(InventarioDespachoPort.class);
        ciclos = mock(CicloDespachoRepository.class);
        when(ciclos.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio = new DespacharOrdenService(ordenes, inventario, ciclos, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    void despachaElSiguienteCicloYLiberaSoloLaReservaQueQueda() {
        // Reabierta: pide 6 y le quedan 2 de reserva; el CD tiene 10.
        when(ordenes.iniciarDespacho(ordenId)).thenReturn(new OrdenADespachar(cdId, varianteId, 6, 2));
        when(inventario.aLaManoEnCd(cdId, varianteId)).thenReturn(10);
        when(ciclos.ultimoNumeroCiclo(ordenId)).thenReturn(1);

        CicloDespacho ciclo = servicio.despachar(ordenId);

        assertThat(ciclo.getNumeroCiclo()).isEqualTo(2);
        assertThat(ciclo.getCantidadDespachada()).isEqualTo(6);
        assertThat(ciclo.getFechaDespacho()).isEqualTo(ahora);
        InOrder orden = inOrder(ciclos, inventario, ordenes);
        orden.verify(ciclos).guardar(any());
        orden.verify(inventario).registrarDespacho(cdId, varianteId, 6, 2, ciclo.getId());
        orden.verify(ordenes).confirmarDespacho(ordenId, 6);
    }

    @Test
    void sinStockNoGuardaCicloNiMueveInventarioNiOrden() {
        when(ordenes.iniciarDespacho(ordenId)).thenReturn(new OrdenADespachar(cdId, varianteId, 10, 10));
        when(inventario.aLaManoEnCd(cdId, varianteId)).thenReturn(0);

        assertThatThrownBy(() -> servicio.despachar(ordenId)).isInstanceOf(CdSinStockParaDespacharException.class);

        verify(ciclos, never()).guardar(any());
        verify(inventario, never()).registrarDespacho(any(), any(), anyInt(), anyInt(), any());
        verify(ordenes, never()).confirmarDespacho(any(), anyInt());
    }
}
